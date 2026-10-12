package bible.dbgen

import java.io.File
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    require(args.size == 2) { "usage: dbgen <source-dir> <out.db>" }
    val source = File(args[0])
    val out = File(args[1])
    val books = BookCatalog.books
    val verses = books.associate { b -> b.id to UsfmParser.parse(File(source, b.file).readText()) }
    val errors = validate(books, verses)
    if (errors.isNotEmpty()) {
        errors.forEach { System.err.println(it) }
        exitProcess(1)
    }
    writeDatabase(out, books, verses)
    println("dbgen: ${books.size} книг, ${verses.values.sumOf { it.size }} стихов, ${out.length() / 1024} КБ → ${out.path}")
}
