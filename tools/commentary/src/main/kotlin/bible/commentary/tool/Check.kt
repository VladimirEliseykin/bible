package bible.commentary.tool

import java.io.File
import kotlin.system.exitProcess

/**
 * Полная проверка глав для авторов объяснений: структура (как при сборке) и дословность цитат по корпусу
 * corpus/<книга>/<глава>/ и corpus/<книга>/000/ (все .md внутри: стихи, библиотека lib/, сохранённые вручную extra/).
 * Аргументы: bible.db, notes, sources.json, corpus, необязательно книга и глава («Мф» «5»).
 */
fun main(args: Array<String>) {
    require(args.size >= 4) { "usage: check <bible.db> <notesDir> <sources.json> <corpusDir> [book] [chapter]" }
    val (bibleDb, notesDir, sourcesFile, corpus) = args.take(4).map(::File)
    val book = args.getOrNull(4)?.takeIf { it.isNotEmpty() }
    val chapter = args.getOrNull(5)?.takeIf { it.isNotEmpty() }?.toInt()
    val inputs = Inputs(bibleDb, notesDir, sourcesFile)
    val pd = inputs.sources.associate { it.id to it.public_domain }
    val selected = inputs.files.filter { (book == null || it.book == book) && (chapter == null || it.chapter == chapter) }
    val errors = inputs.errors().filter { e -> selected.any { e.startsWith("${it.book} ${it.chapter}") } || e.startsWith("sources.json") }.toMutableList()
    for (f in selected) {
        val dir = File(corpus, "${f.book}/%03d".format(f.chapter))
        // Введения и книги целиком (000) нужны для цитат в общем описании главы.
        val text = (dir.walkTopDown() + File(corpus, "${f.book}/000").walkTopDown()).filter { it.isFile && it.extension == "md" }.sorted().joinToString("\n") { it.readText() }
        if (text.isEmpty() && f.verses.values.any { it.fathers.isNotEmpty() }) errors += "${f.book} ${f.chapter}: нет корпуса в $dir"
        else errors += checkQuotes(f, text, pd)
    }
    errors.forEach { System.err.println(it) }
    println("check: ${selected.size} глав, ошибок ${errors.size}")
    if (errors.isNotEmpty()) exitProcess(1)
}
