package bible.commentary.tool

import java.io.File

/**
 * Скачивает библиотеку источников по списку: строки «адрес<TAB>путь» (путь относительно [base]).
 * Уже скачанное пропускается; ошибки не останавливают загрузку, а перечисляются в конце.
 * Аргументы: urls.tsv, базовый каталог (tools/commentary).
 */
fun main(args: Array<String>) {
    require(args.size == 2) { "usage: fetchList <urls.tsv> <baseDir>" }
    val base = File(args[1])
    val lines = File(args[0]).readLines().map { it.split('\t') }.filter { it.size == 2 && it[0].startsWith("http") }
    val failed = mutableListOf<String>()
    var done = 0
    for ((url, path) in lines) {
        val out = File(base, path)
        if (out.exists()) continue
        try {
            savePage(url, out)
            done++
        } catch (e: Exception) {
            failed += "$url – ${e.message}"
        }
        Thread.sleep(500)
    }
    println("library: скачано $done, уже было ${lines.size - done - failed.size}, ошибок ${failed.size}")
    failed.forEach { System.err.println(it) }
}
