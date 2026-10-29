package bible.commentary.tool

import bible.commentary.model.ChapterNote
import bible.commentary.model.Item
import bible.commentary.model.Reading
import bible.commentary.model.decodeText
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
            readings = mapOf(27002001 to listOf(Reading("А. П. Лопухин", "Толковая Библия", "Первый абзац.\n\nВторой."), Reading("Троицкие листки", "", "", see = 27002002))),
            sources = listOf(Source("lopukhin", "Толковая Библия", "А. П. Лопухин", "https://azbyka.ru", "public domain")),
            version = 3,
        )
        DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT body FROM verse_note WHERE verse_id=27002001").use { r -> r.next(); assertEquals(note, decodeNote(r.getBytes(1))) }
                s.executeQuery("SELECT body FROM chapter_note WHERE book_id=27 AND chapter=2").use { r -> r.next(); assertEquals("Сирах", decodeChapter(r.getBytes(1)).context.single().text) }
                s.executeQuery("SELECT author, work, body FROM reading WHERE verse_id=27002001 AND ord=0").use { r ->
                    r.next()
                    assertEquals("А. П. Лопухин", r.getString(1))
                    assertEquals("Первый абзац.\n\nВторой.", decodeText(r.getBytes(3)))
                }
                s.executeQuery("SELECT see FROM reading WHERE verse_id=27002001 AND ord=1").use { r -> r.next(); assertEquals(27002002, r.getInt(1)) }
                s.executeQuery("SELECT value FROM meta WHERE key='version'").use { r -> r.next(); assertEquals("3", r.getString(1)) }
                s.executeQuery("SELECT title FROM source WHERE id='lopukhin'").use { r -> r.next(); assertEquals("Толковая Библия", r.getString(1)) }
            }
        }
    }
}

class ShortDashTest {
    @Test fun longDashesBecomeShortInNotesAndReadings() {
        val out = File.createTempFile("commentary", ".db").apply { delete() }
        writePack(
            out,
            notes = mapOf(27002001 to VerseNote(lead = "Кратко", fathers = listOf(bible.commentary.model.Father("Ф", "Это — цитата", "lopukhin")))),
            chapters = emptyMap(),
            readings = mapOf(27002001 to listOf(Reading("Лопухин", "Толковая — Библия", "Текст — толкования"))),
            sources = emptyList(),
            version = 1,
        )
        DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT body FROM verse_note").use { r -> r.next(); assertEquals("Это – цитата", decodeNote(r.getBytes(1)).fathers.single().quote) }
                s.executeQuery("SELECT work, body FROM reading").use { r ->
                    r.next()
                    assertEquals("Толковая – Библия", r.getString(1))
                    assertEquals("Текст – толкования", decodeText(r.getBytes(2)))
                }
            }
        }
    }
}
