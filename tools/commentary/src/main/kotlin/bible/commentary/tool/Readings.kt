package bible.commentary.tool

import bible.commentary.model.ChapterReadings
import bible.commentary.model.Json
import bible.commentary.model.Reading
import java.io.File
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

/** Автор с bible.optina.ru: можно ли класть его текст в приложение и как его называть (optina-authors.json). */
@Serializable
data class OptinaAuthor(val public_domain: Boolean, val author: String, val translation: String = "", val evidence: String = "")

/** Файл корпуса (формат [corpusText]) обратно: текст стиха и разделы авторов. */
fun parseCorpus(md: String): Pair<String, List<OptinaSection>> {
    val lines = md.lines()
    val verse = lines.firstOrNull { it.startsWith("> ") }?.removePrefix("> ").orEmpty()
    val sections = mutableListOf<OptinaSection>()
    var author: String? = null
    var work = ""
    val paragraphs = mutableListOf<String>()
    fun flush() {
        author?.let { sections += OptinaSection(it, paragraphs.toList(), work) }
        paragraphs.clear()
        work = ""
    }
    for (line in lines) {
        when {
            line.startsWith("## ") -> { flush(); author = line.removePrefix("## ").trim() }
            author == null || line.isBlank() || line.startsWith("Источник: ") -> Unit
            line.startsWith("_") && line.endsWith("_") && work.isEmpty() && paragraphs.isEmpty() -> work = line.trim('_')
            else -> paragraphs += line.trim()
        }
    }
    flush()
    return verse to sections
}

/**
 * Толкования стиха для приложения: только авторы в общественном достоянии, имена в едином виде,
 * без первого абзаца, если он лишь повторяет текст стиха.
 */
fun readings(md: String, authors: Map<String, OptinaAuthor>): List<Reading> {
    val (verse, sections) = parseCorpus(md)
    return sections.mapNotNull { s ->
        val a = authors[s.author]?.takeIf { it.public_domain } ?: return@mapNotNull null
        val body = if (s.paragraphs.size > 1 && repeatsVerse(s.paragraphs.first(), verse)) s.paragraphs.drop(1) else s.paragraphs
        val see = body.singleOrNull()?.let(::seeAlso)
        when {
            see != null -> Reading(a.author, "", "", see)
            body.isEmpty() -> null
            else -> Reading(a.author, s.work.trimEnd('.', ' '), body.joinToString("\n\n"))
        }
    }
}

/** Книги, на которые ссылаются заглушки bible.optina.ru (номера как в bible.db). */
private val SEE_BOOKS = mapOf("Мф" to 51, "Мк" to 52, "Лк" to 53, "Ин" to 54, "Сир" to 27, "Прем. Сир" to 27, "Прем. Сираха" to 27)

/** «См. Толкование на Мф. 5:2» → id стиха 51005002; не заглушка → null. Берётся первое место. */
fun seeAlso(text: String): Int? {
    val m = Regex("""^См\.\s*[Тт]олкование\s+(?:на\s+)?(Прем\. Сираха|Прем\. Сир|Мф|Мк|Лк|Ин|Сир)\.?\s*(\d+)\s*[:,]\s*(\d+)""").find(text.trim()) ?: return null
    val book = SEE_BOOKS[m.groupValues[1]] ?: return null
    return book * 1_000_000 + m.groupValues[2].toInt() * 1_000 + m.groupValues[3].toInt()
}

private fun words(s: String) = s.lowercase().replace('ё', 'е').split(Regex("[^\\p{L}]+")).filter { it.length > 2 }

private fun repeatsVerse(paragraph: String, verse: String): Boolean {
    val p = words(paragraph)
    val v = words(verse).toSet()
    return p.isNotEmpty() && p.size <= v.size + 3 && p.count { it in v } >= p.size * 0.6
}

/**
 * Собирает readings/<книга>/<глава>.json из корпуса bible.optina.ru по списку авторов.
 * Аргументы: каталог корпуса, optina-authors.json, каталог readings, книги через запятую («Мф,Сир»).
 */
fun main(args: Array<String>) {
    require(args.size == 4) { "usage: readings <corpusDir> <optina-authors.json> <outDir> <books>" }
    val (corpus, authorsFile, out) = args.take(3).map(::File)
    val authors = Json.decodeFromString(MapSerializer(String.serializer(), OptinaAuthor.serializer()), authorsFile.readText())
    var total = 0
    for (book in args[3].split(',')) {
        File(corpus, book).listFiles { f -> f.isDirectory && f.name.matches(Regex("\\d{3}")) && f.name != "000" }!!.sorted().forEach { dir ->
            val verses = dir.listFiles { f -> f.isFile && f.name.matches(Regex("\\d{3}\\.md")) }!!.sorted()
                .associate { it.name.take(3).toInt().toString() to readings(it.readText(), authors) }
                .filterValues { it.isNotEmpty() }
            total += verses.values.sumOf { it.size }
            val file = File(out, "$book/${dir.name}.json").apply { parentFile.mkdirs() }
            file.writeText(Json.encodeToString(ChapterReadings.serializer(), ChapterReadings(book, dir.name.toInt(), verses)))
        }
    }
    println("readings: $total толкований → $out")
}
