package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.VerseNote
import bible.commentary.model.encodeChapter
import bible.commentary.model.encodeNote
import java.io.File
import java.sql.DriverManager
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class Source(val id: String, val title: String, val author: String, val url: String, val license: String)

fun writePack(
    out: File,
    notes: Map<Int, VerseNote>,
    chapters: Map<Pair<Int, Int>, ChapterNote>,
    csl: Map<Int, String>,
    sources: List<Source>,
    version: Int,
) {
    out.parentFile?.mkdirs()
    out.delete()
    DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c ->
        c.autoCommit = false
        c.createStatement().use { s ->
            s.executeUpdate("CREATE TABLE meta(key TEXT PRIMARY KEY, value TEXT NOT NULL)")
            s.executeUpdate("CREATE TABLE verse_note(verse_id INTEGER PRIMARY KEY, body BLOB NOT NULL)")
            s.executeUpdate("CREATE TABLE chapter_note(book_id INTEGER NOT NULL, chapter INTEGER NOT NULL, body BLOB NOT NULL, PRIMARY KEY(book_id, chapter))")
            s.executeUpdate("CREATE TABLE translation(verse_id INTEGER NOT NULL, code TEXT NOT NULL, text TEXT NOT NULL, PRIMARY KEY(verse_id, code))")
            s.executeUpdate("CREATE TABLE source(id TEXT PRIMARY KEY, title TEXT NOT NULL, author TEXT NOT NULL, url TEXT NOT NULL, license TEXT NOT NULL)")
        }
        c.prepareStatement("INSERT INTO meta VALUES(?,?)").use { p ->
            listOf("version" to version.toString(), "built_at" to java.time.Instant.now().toString()).forEach { (k, v) -> p.setString(1, k); p.setString(2, v); p.addBatch() }
            p.executeBatch()
        }
        c.prepareStatement("INSERT INTO verse_note VALUES(?,?)").use { p ->
            notes.toSortedMap().forEach { (id, n) -> p.setInt(1, id); p.setBytes(2, encodeNote(n)); p.addBatch() }
            p.executeBatch()
        }
        c.prepareStatement("INSERT INTO chapter_note VALUES(?,?,?)").use { p ->
            chapters.forEach { (k, n) -> p.setInt(1, k.first); p.setInt(2, k.second); p.setBytes(3, encodeChapter(n)); p.addBatch() }
            p.executeBatch()
        }
        c.prepareStatement("INSERT INTO translation VALUES(?,?,?)").use { p ->
            csl.toSortedMap().forEach { (id, t) -> p.setInt(1, id); p.setString(2, "csl"); p.setString(3, t); p.addBatch() }
            p.executeBatch()
        }
        c.prepareStatement("INSERT INTO source VALUES(?,?,?,?,?)").use { p ->
            sources.forEach { s -> p.setString(1, s.id); p.setString(2, s.title); p.setString(3, s.author); p.setString(4, s.url); p.setString(5, s.license); p.addBatch() }
            p.executeBatch()
        }
        c.commit()
    }
    DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c -> c.createStatement().use { it.executeUpdate("VACUUM") } }
}

/**
 * Церковнославянский текст (Елизаветинская Библия, JSON из asdf-a11/ChurchSlavonicBibleInUtf8).
 * Книги идут в том же православном порядке, что и в bible.db; берутся только стихи, существующие в bible.db.
 */
fun loadCsl(file: File, bookChapters: Map<Int, Int>, verseIds: Set<Int>): Map<Int, String> {
    val books = Json.parseToJsonElement(file.readText()).jsonObject.getValue("books").jsonArray
    require(books.size == bookChapters.size) { "CSL: ${books.size} книг, ожидалось ${bookChapters.size}" }
    val out = HashMap<Int, String>()
    books.forEachIndexed { i, b ->
        val bookId = i + 1
        for (ch in b.jsonObject.getValue("chapters").jsonArray) {
            val chapter = ch.jsonObject.getValue("chapter").jsonPrimitive.int
            for (v in ch.jsonObject.getValue("verses").jsonArray) {
                val id = bookId * 1_000_000 + chapter * 1_000 + v.jsonObject.getValue("verse").jsonPrimitive.int
                if (id in verseIds) out[id] = v.jsonObject.getValue("text").jsonPrimitive.content.trim()
            }
        }
    }
    return out
}
