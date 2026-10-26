package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.ChapterReadings
import bible.commentary.model.Json
import bible.commentary.model.Reading
import bible.commentary.model.VerseNote
import java.io.File
import kotlin.system.exitProcess

/** Аргументы: bible.db, каталог notes, sources.json, каталог readings, путь к commentary.db, версия данных. */
fun main(args: Array<String>) {
    require(args.size == 6) { "usage: commentary <bible.db> <notesDir> <sources.json> <readingsDir> <out.db> <version>" }
    val (bibleDb, notesDir, sourcesFile, readingsDir, db) = args.take(5).map(::File)
    val version = args[5].toInt()

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

    val readings = HashMap<Int, List<Reading>>()
    readingsDir.walkTopDown().filter { it.isFile && it.extension == "json" }.forEach { file ->
        val r = Json.decodeFromString(ChapterReadings.serializer(), file.readText())
        val book = bookIds[r.book] ?: error("$file: неизвестная книга ${r.book}")
        r.verses.forEach { (n, list) ->
            val id = book * 1_000_000 + r.chapter * 1_000 + n.toInt()
            if (id in inputs.verseIds) readings[id] = list else System.err.println("$file: стиха $n нет в bible.db, пропущен")
        }
    }

    writePack(db, notes, chapters, readings, sources.map { Source(it.id, it.title, it.author, it.url, it.license) }, version)
    println("commentary: ${notes.size} стихов, ${chapters.size} глав, ${readings.values.sumOf { it.size }} толкований, ${db.length() / 1024} КБ → $db")
}
