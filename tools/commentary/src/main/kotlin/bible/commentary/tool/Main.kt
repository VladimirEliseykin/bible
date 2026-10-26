package bible.commentary.tool

import bible.commentary.model.ChapterFile
import bible.commentary.model.ChapterNote
import bible.commentary.model.Json
import bible.commentary.model.VerseNote
import java.io.File
import java.security.MessageDigest
import java.sql.DriverManager
import java.util.zip.GZIPOutputStream
import kotlin.system.exitProcess
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.Serializable

@Serializable private data class SourceJson(val id: String, val title: String, val author: String, val url: String, val license: String)

/** Аргументы: bible.db, каталог notes, sources.json, CSlElizabeth-CS.json, выходной каталог, версия пакета. */
fun main(args: Array<String>) {
    require(args.size == 6) { "usage: commentary <bible.db> <notesDir> <sources.json> <csl.json> <outDir> <version>" }
    val (bibleDb, notesDir, sourcesFile, cslFile, outDir) = args.take(5).map(::File)
    val version = args[5].toInt()

    val verseIds = HashSet<Int>()
    val bookIds = HashMap<String, Int>()
    val bookChapters = HashMap<Int, Int>()
    DriverManager.getConnection("jdbc:sqlite:${bibleDb.path}").use { c ->
        c.createStatement().use { s ->
            s.executeQuery("SELECT id FROM verse").use { r -> while (r.next()) verseIds += r.getInt(1) }
            s.executeQuery("SELECT id, abbr, chapters FROM book").use { r -> while (r.next()) { bookIds[r.getString(2)] = r.getInt(1); bookChapters[r.getInt(1)] = r.getInt(3) } }
        }
    }
    val sources = Json.decodeFromString(ListSerializer(SourceJson.serializer()), sourcesFile.readText())
    val files = notesDir.walkTopDown().filter { it.isFile && it.extension == "json" }.sorted()
        .map { Json.decodeFromString(ChapterFile.serializer(), it.readText()) }.toList()

    val errors = validate(files, verseIds, bookIds, sources.map { it.id }.toSet())
    if (errors.isNotEmpty()) {
        errors.forEach { System.err.println(it) }
        exitProcess(1)
    }

    val notes = HashMap<Int, VerseNote>()
    val chapters = HashMap<Pair<Int, Int>, ChapterNote>()
    for (f in files) {
        val book = bookIds.getValue(f.book)
        if (f.chapter_note.context.isNotEmpty()) chapters[book to f.chapter] = f.chapter_note
        f.verses.forEach { (n, note) -> notes[book * 1_000_000 + f.chapter * 1_000 + n.toInt()] = note }
    }
    val csl = loadCsl(cslFile, bookChapters, verseIds)

    val db = File(outDir, "commentary.db")
    writePack(db, notes, chapters, csl, sources.map { Source(it.id, it.title, it.author, it.url, it.license) }, version)
    val gz = File(outDir, "commentary.db.gz")
    GZIPOutputStream(gz.outputStream()).use { out -> db.inputStream().use { it.copyTo(out) } }
    val sha = MessageDigest.getInstance("SHA-256").digest(gz.readBytes()).joinToString("") { "%02x".format(it) }
    File(outDir, "commentary.json").writeText(
        """{"version":$version,"url":"commentary.db.gz","size":${gz.length()},"sha256":"$sha","bytes_unpacked":${db.length()}}""" + "\n",
    )
    println("commentary: ${notes.size} стихов, ${chapters.size} глав, ${csl.size} стихов ЦС, ${gz.length() / 1024} КБ сжато → $outDir")
}
