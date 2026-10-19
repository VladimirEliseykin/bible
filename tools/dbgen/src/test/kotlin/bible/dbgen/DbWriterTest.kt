package bible.dbgen

import java.io.File
import java.sql.DriverManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DbWriterTest {
    private val book = BookCatalog.books.first { it.abbr == "Руф" }

    @Test fun writesBooksVersesAndSearchIndex() {
        val file = File.createTempFile("bible", ".db").apply { delete() }
        writeDatabase(file, listOf(book), mapOf(book.id to listOf(ParsedVerse(1, 1, "Любовью свет."), ParsedVerse(1, 2, "Тьма."))))
        DriverManager.getConnection("jdbc:sqlite:${file.path}").use { c ->
            c.createStatement().use { s ->
                s.executeQuery("SELECT count(*) FROM verse").use { r -> r.next(); assertEquals(2, r.getInt(1)) }
                s.executeQuery("SELECT id FROM verse WHERE verse = 2").use { r -> r.next(); assertEquals(book.id * 1_000_000 + 1_000 + 2, r.getInt(1)) }
                s.executeQuery("SELECT docid FROM verse_fts WHERE verse_fts MATCH '${TextNormalizer.stems("любовь")}'").use { r ->
                    r.next(); assertEquals(book.id * 1_000_000 + 1_001, r.getInt(1))
                }
                s.executeQuery("PRAGMA user_version").use { r -> r.next(); assertEquals(DB_VERSION, r.getInt(1)) }
            }
        }
    }

    @Test fun validationReportsMissingChapterAndEmptyVerse() {
        val errors = validate(listOf(book), mapOf(book.id to listOf(ParsedVerse(1, 1, ""), ParsedVerse(2, 1, "x"))))
        assertTrue(errors.any { it.contains("Руф") && it.contains("глав") })
        assertTrue(errors.any { it.contains("пуст") })
    }

    @Test fun validationReportsDuplicateVerse() {
        val verses = (1..4).map { ParsedVerse(it, 1, "a") } + ParsedVerse(1, 1, "b")
        assertTrue(validate(listOf(book), mapOf(book.id to verses)).any { it.contains("повтор") })
    }

    @Test fun validationReportsGapInVerseNumbers() {
        val verses = (1..4).map { ParsedVerse(it, 1, "a") } + ParsedVerse(1, 3, "c")
        assertTrue(validate(listOf(book), mapOf(book.id to verses)).any { it.contains("пропуск") })
    }
}
