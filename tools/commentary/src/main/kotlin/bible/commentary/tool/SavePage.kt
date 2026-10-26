package bible.commentary.tool

import java.io.File
import org.jsoup.Jsoup

/**
 * Сохраняет текст веб-страницы дословно в корпус, чтобы цитаты из неё проходили проверку.
 * Аргументы: адрес, файл .md (например corpus/Мф/005/extra/zlatoust-15.md).
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: save <url> <out.md>" }
    val (url, out) = args
    val doc = Jsoup.connect(url).userAgent("Mozilla/5.0 (bible-app commentary builder)").timeout(60_000).get()
    val text = doc.select("p, h1, h2, h3, h4, blockquote, li").joinToString("\n\n") { it.text() }.ifBlank { doc.body().text() }
    File(out).apply { parentFile.mkdirs() }.writeText("# ${doc.title()}\n\nИсточник: $url\n\n$text\n")
    println("saved ${text.length} chars → $out")
}
