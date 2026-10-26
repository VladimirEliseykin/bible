package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.Item
import bible.commentary.model.VerseNote
import bible.commentary.model.decodeChapter
import bible.commentary.model.decodeNote
import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Test

class PackWriterTest {
    @Test fun writesNotesChaptersAndSources() {
        val out = File.createTempFile("commentary", ".db").apply { delete() }
        val note = VerseNote(lead = "Кратко", meaning = listOf(Item("a", "b")))
        writePack(
            out,
            notes = mapOf(27002001 to note),
            chapters = mapOf((27 to 2) to ChapterNote(listOf(Item("Кто", "Сирах", src = listOf("lopukhin"))))),
            sources = listOf(Source("lopukhin", "Толковая Библия", "А. П. Лопухин", "https://azbyka.ru", "public domain")),
            version = 3,
        )
        DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT body FROM verse_note WHERE verse_id=27002001").use { r -> r.next(); assertEquals(note, decodeNote(r.getBytes(1))) }
                s.executeQuery("SELECT body FROM chapter_note WHERE book_id=27 AND chapter=2").use { r -> r.next(); assertEquals("Сирах", decodeChapter(r.getBytes(1)).context.single().text) }
                s.executeQuery("SELECT value FROM meta WHERE key='version'").use { r -> r.next(); assertEquals("3", r.getString(1)) }
                s.executeQuery("SELECT title FROM source WHERE id='lopukhin'").use { r -> r.next(); assertEquals("Толковая Библия", r.getString(1)) }
            }
        }
    }
}
