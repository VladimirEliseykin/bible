package bible.dbgen

import java.io.File
import java.sql.DriverManager

const val DB_VERSION = 1

fun verseId(book: Int, chapter: Int, verse: Int) = book * 1_000_000 + chapter * 1_000 + verse

fun validate(books: List<BookInfo>, verses: Map<Int, List<ParsedVerse>>): List<String> {
    val errors = mutableListOf<String>()
    for (b in books) {
        val list = verses[b.id].orEmpty()
        val chapters = list.map { it.chapter }.toSortedSet()
        if (chapters.toList() != (1..b.chapters).toList()) {
            errors += "${b.abbr}: ожидалось глав ${b.chapters}, найдено ${chapters.size} ($chapters)"
        }
        list.filter { it.text.isBlank() }.forEach { errors += "${b.abbr} ${it.chapter}:${it.verse}: пустой стих" }
        list.groupBy { it.chapter to it.verse }.filterValues { it.size > 1 }.keys
            .forEach { (c, v) -> errors += "${b.abbr} $c:$v: повтор номера стиха" }
        list.groupBy { it.chapter }.forEach { (c, vs) ->
            val numbers = vs.map { it.verse }.toSortedSet().toList()
            if (numbers != (1..numbers.size).toList()) errors += "${b.abbr} $c: пропуск в номерах стихов ($numbers)"
        }
        list.filter { it.verse > 999 || it.chapter > 999 }
            .forEach { errors += "${b.abbr} ${it.chapter}:${it.verse}: номер вне диапазона id" }
    }
    return errors
}

fun writeDatabase(out: File, books: List<BookInfo>, verses: Map<Int, List<ParsedVerse>>) {
    out.parentFile?.mkdirs()
    out.delete()
    DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c ->
        c.autoCommit = false
        c.createStatement().use { s ->
            s.executeUpdate("CREATE TABLE book(id INTEGER PRIMARY KEY, testament INTEGER NOT NULL, grp TEXT NOT NULL, title TEXT NOT NULL, short_title TEXT NOT NULL, abbr TEXT NOT NULL, chapters INTEGER NOT NULL, deutero INTEGER NOT NULL)")
            s.executeUpdate("CREATE TABLE verse(id INTEGER PRIMARY KEY, book_id INTEGER NOT NULL, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, text TEXT NOT NULL)")
            s.executeUpdate("CREATE INDEX verse_chapter ON verse(book_id, chapter, verse)")
            s.executeUpdate("CREATE VIRTUAL TABLE verse_fts USING fts4(content=\"\", stem)")
        }
        c.prepareStatement("INSERT INTO book VALUES(?,?,?,?,?,?,?,?)").use { p ->
            for (b in books) {
                p.setInt(1, b.id)
                p.setInt(2, if (b.testament == Testament.OLD) 0 else 1)
                p.setString(3, b.group.name)
                p.setString(4, b.title)
                p.setString(5, b.shortTitle)
                p.setString(6, b.abbr)
                p.setInt(7, b.chapters)
                p.setInt(8, if (b.deuterocanonical) 1 else 0)
                p.addBatch()
            }
            p.executeBatch()
        }
        c.prepareStatement("INSERT INTO verse VALUES(?,?,?,?,?)").use { pv ->
            c.prepareStatement("INSERT INTO verse_fts(docid, stem) VALUES(?,?)").use { pf ->
                for (b in books) for (v in verses[b.id].orEmpty()) {
                    val id = verseId(b.id, v.chapter, v.verse)
                    pv.setInt(1, id); pv.setInt(2, b.id); pv.setInt(3, v.chapter); pv.setInt(4, v.verse); pv.setString(5, TextNormalizer.dashes(v.text))
                    pv.addBatch()
                    pf.setInt(1, id); pf.setString(2, TextNormalizer.stems(v.text))
                    pf.addBatch()
                }
                pv.executeBatch()
                pf.executeBatch()
            }
        }
        c.createStatement().use { s ->
            s.executeUpdate("INSERT INTO verse_fts(verse_fts) VALUES('optimize')")
            s.executeUpdate("PRAGMA user_version = $DB_VERSION")
        }
        c.commit()
    }
    DriverManager.getConnection("jdbc:sqlite:${out.path}").use { c -> c.createStatement().use { it.executeUpdate("VACUUM") } }
}
