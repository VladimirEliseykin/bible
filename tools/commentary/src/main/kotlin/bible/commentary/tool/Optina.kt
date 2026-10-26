package bible.commentary.tool

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.sql.DriverManager
import org.jsoup.Jsoup

/** Толкование одного автора на странице стиха bible.optina.ru: абзацы дословно и название сочинения. */
data class OptinaSection(val author: String, val paragraphs: List<String>, val work: String)

/** Разделы авторов со страницы стиха. Оглавление и пустые страницы дают пустой список. */
fun parseOptina(html: String): List<OptinaSection> {
    val doc = Jsoup.parse(html)
    return doc.select("h2[id]").mapNotNull { h ->
        val body = h.nextElementSibling()?.takeIf { it.hasClass("level2") } ?: return@mapNotNull null
        val ps = body.select("p").map { p ->
            val text = p.text().trim()
            val isWork = text.isNotEmpty() && p.select("em").text() == text && p.select("strong").text() == text
            text to isWork
        }.filter { it.first.isNotEmpty() }
        val work = ps.lastOrNull()?.takeIf { it.second && ps.size > 1 }?.first.orEmpty()
        val paragraphs = (if (work.isEmpty()) ps else ps.dropLast(1)).map { it.first }
        OptinaSection(h.text().trim(), paragraphs, work)
    }
}

/** Файл корпуса на стих: заголовок, текст стиха, затем авторы с сочинениями и абзацами. */
fun corpusText(title: String, verse: String, sections: List<OptinaSection>): String = buildString {
    append("# ").append(title).append("\n\n> ").append(verse).append("\n\n")
    if (sections.isEmpty()) append("(толкований на сайте нет)\n")
    for (s in sections) {
        append("## ").append(s.author).append('\n')
        if (s.work.isNotEmpty()) append('_').append(s.work).append("_\n")
        append('\n')
        s.paragraphs.forEach { append(it).append("\n\n") }
    }
}

/**
 * Скачивает толкования с bible.optina.ru по стихам глав в corpus/<книга>/<глава>/<стих>.md.
 * Аргументы: bible.db, каталог корпуса, сокращение книги (Мф), код книги на сайте (new:mf), главы («5» или «1-28»).
 * Уже скачанные стихи пропускаются: прерванную загрузку можно продолжить.
 */
fun main(args: Array<String>) {
    require(args.size == 5) { "usage: fetch <bible.db> <corpusDir> <abbr> <optinaCode> <chapters>" }
    val (bibleDb, corpus) = args.take(2).map(::File)
    val (abbr, code, range) = args.drop(2)
    val chapters = range.split('-').map(String::toInt).let { if (it.size == 1) it[0]..it[0] else it[0]..it[1] }
    val verses = DriverManager.getConnection("jdbc:sqlite:${bibleDb.path}").use { c ->
        c.prepareStatement("SELECT v.id, v.text FROM verse v JOIN book b ON b.id = v.id / 1000000 WHERE b.abbr = ? ORDER BY v.id").use { p ->
            p.setString(1, abbr)
            p.executeQuery().use { r -> buildList { while (r.next()) add(r.getInt(1) to r.getString(2)) } }
        }
    }
    require(verses.isNotEmpty()) { "нет книги $abbr в bible.db" }
    var fetched = 0
    for ((id, text) in verses) {
        val ch = id / 1_000 % 1_000
        val v = id % 1_000
        if (ch !in chapters) continue
        val out = File(corpus, "$abbr/%03d/%03d.md".format(ch, v))
        if (out.exists()) continue
        val url = "https://bible.optina.ru/$code:%02d:%02d".format(ch, v)
        val sections = parseOptina(get(url))
        out.parentFile.mkdirs()
        out.writeText(corpusText("$abbr $ch:$v", text, sections) + "\nИсточник: $url\n")
        fetched++
        Thread.sleep(300)
    }
    println("optina: $abbr $range – скачано $fetched стихов → $corpus/$abbr")
}

private fun get(url: String): String {
    var last: Exception? = null
    repeat(3) { attempt ->
        try {
            val c = URI(url).toURL().openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "Mozilla/5.0 (bible-app commentary builder)")
            c.connectTimeout = 20_000
            c.readTimeout = 60_000
            if (c.responseCode == 404) return ""
            if (c.responseCode !in 200..299) throw IllegalStateException("HTTP ${c.responseCode} $url")
            return c.inputStream.use { it.readBytes().decodeToString() }
        } catch (e: Exception) {
            last = e
            Thread.sleep(2_000L * (attempt + 1))
        }
    }
    throw last!!
}
