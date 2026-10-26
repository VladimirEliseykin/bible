package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.VerseNote
import java.io.File
import kotlin.system.exitProcess

/** Аргументы: bible.db, каталог notes, sources.json, путь к commentary.db, версия данных. */
fun main(args: Array<String>) {
    require(args.size == 5) { "usage: commentary <bible.db> <notesDir> <sources.json> <out.db> <version>" }
    val (bibleDb, notesDir, sourcesFile, db) = args.take(4).map(::File)
    val version = args[4].toInt()

    val inputs = Inputs(bibleDb, notesDir, sourcesFile)
    val (files, bookIds, sources) = Triple(inputs.files, inputs.bookIds, inputs.sources)
    val errors = inputs.errors()
    if (errors.isNotEmpty()) {
        errors.forEach { System.err.println(it) }
        exitProcess(1)
    }

    val notes = HashMap<Int, VerseNote>()
    val chapters = HashMap<Pair<Int, Int>, ChapterNote>()
    for (f in files) {
        val book = bookIds.getValue(f.book)
        if (f.chapter_note != ChapterNote()) chapters[book to f.chapter] = f.chapter_note
        f.verses.forEach { (n, note) -> notes[book * 1_000_000 + f.chapter * 1_000 + n.toInt()] = note }
    }

    writePack(db, notes, chapters, sources.map { Source(it.id, it.title, it.author, it.url, it.license) }, version)
    println("commentary: ${notes.size} стихов, ${chapters.size} глав, ${db.length() / 1024} КБ → $db")
}
