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
        // Медленные сайты (ekzeget.ru) иногда не отвечают вовремя: три попытки с паузой.
        var error: Exception? = null
        for (attempt in 1..3) {
            error = try {
                savePage(url, out)
                null
            } catch (e: Exception) {
                e
            }
            if (error == null) break
            Thread.sleep(5_000L * attempt)
        }
        if (error == null) done++ else failed += "$url – ${error.message}"
        Thread.sleep(500)
    }
    println("library: скачано $done, уже было ${lines.size - done - failed.size}, ошибок ${failed.size}")
    failed.forEach { System.err.println(it) }
}
