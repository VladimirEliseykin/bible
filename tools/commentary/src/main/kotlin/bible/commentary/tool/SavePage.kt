package bible.commentary.tool

import java.io.File
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Весь текст страницы без меню и скриптов, по абзацам: блочные элементы – с новой строки. */
fun pageText(doc: Document): String {
    doc.select("script, style, noscript, nav, header, footer, form, iframe, svg").remove()
    return blockText(doc.body())
}

/** Толкования со страницы стиха ekzeget.ru: автор, группа стихов, текст – без меню и навигации (они весят сотни КБ). */
fun ekzegetText(doc: Document): String = doc.select(
    ".interpretation_detail__content__leftSide-title, .interpretation_detail__content__group, .interpretation_detail__content__leftSide-text",
).joinToString("") { e ->
    when {
        e.hasClass("interpretation_detail__content__leftSide-title") -> "\n\n## " + e.text().trim() + "\n"
        e.hasClass("interpretation_detail__content__group") -> "_" + e.text().trim() + "_\n\n"
        else -> blockText(e)
    }
}.trim()

private fun blockText(root: Element): String {
    val out = StringBuilder()
    fun walk(e: Element) {
        for (n in e.childNodes()) {
            when (n) {
                is TextNode -> out.append(n.text())
                is Element -> {
                    val block = n.isBlock || n.tagName() == "br"
                    if (block) out.append('\n')
                    walk(n)
                    if (block) out.append('\n')
                }
            }
        }
    }
    walk(root)
    return out.toString().lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
}

fun savePage(url: String, out: File) {
    val doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (bible-app commentary builder)").timeout(120_000).get()
    val text = if ("ekzeget.ru" in url) ekzegetText(doc).ifEmpty { pageText(doc) } else pageText(doc)
    out.apply { parentFile.mkdirs() }.writeText("# ${doc.title()}\n\nИсточник: $url\n\n$text\n")
}

/**
 * Сохраняет текст веб-страницы дословно в корпус, чтобы цитаты из неё проходили проверку.
 * Аргументы: адрес, файл .md (например corpus/Мф/005/extra/zlatoust-15.md).
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: save <url> <out.md>" }
    savePage(args[0], File(args[1]))
    println("saved → ${args[1]}")
}
