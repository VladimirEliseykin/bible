package bible.commentary.tool

import java.io.File
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.nodes.TextNode

/** Весь текст страницы без меню и скриптов, по абзацам: блочные элементы – с новой строки. */
fun pageText(doc: Document): String {
    doc.select("script, style, noscript, nav, header, footer, form, iframe, svg").remove()
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
    walk(doc.body())
    return out.toString().lines().map { it.replace(Regex("\\s+"), " ").trim() }.filter { it.isNotEmpty() }.joinToString("\n\n")
}

fun savePage(url: String, out: File) {
    val doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (bible-app commentary builder)").timeout(60_000).get()
    out.apply { parentFile.mkdirs() }.writeText("# ${doc.title()}\n\nИсточник: $url\n\n${pageText(doc)}\n")
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
