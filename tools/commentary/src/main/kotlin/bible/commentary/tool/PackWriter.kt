package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.Reading
import bible.commentary.model.VerseNote
import bible.commentary.model.encodeChapter
import bible.commentary.model.encodeNote
import bible.commentary.model.encodeText
import java.io.File
import java.sql.DriverManager

data class Source(val id: String, val title: String, val author: String, val url: String, val license: String)

fun writePack(
    out: File,
    notes: Map<Int, VerseNote>,
    chapters: Map<Pair<Int, Int>, ChapterNote>,
    readings: Map<Int, List<Reading>>,
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
            s.executeUpdate("CREATE TABLE reading(verse_id INTEGER NOT NULL, ord INTEGER NOT NULL, author TEXT NOT NULL, work TEXT NOT NULL, body BLOB NOT NULL, see INTEGER, PRIMARY KEY(verse_id, ord))")
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
        c.prepareStatement("INSERT INTO reading VALUES(?,?,?,?,?,?)").use { p ->
            readings.toSortedMap().forEach { (id, rs) ->
                rs.forEachIndexed { i, r -> p.setInt(1, id); p.setInt(2, i); p.setString(3, r.author); p.setString(4, r.work); p.setBytes(5, encodeText(r.text)); r.see?.let { p.setInt(6, it) } ?: p.setNull(6, java.sql.Types.INTEGER); p.addBatch() }
            }
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
