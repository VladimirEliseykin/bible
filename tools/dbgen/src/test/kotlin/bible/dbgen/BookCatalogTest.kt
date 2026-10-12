package bible.dbgen

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BookCatalogTest {
    private val books = BookCatalog.books

    @Test fun has77BooksWithSequentialIds() {
        assertEquals(77, books.size)
        assertEquals((1..77).toList(), books.map { it.id })
    }

    @Test fun testamentSplitIs50And27() {
        assertEquals(50, books.count { it.testament == Testament.OLD })
        assertEquals(27, books.count { it.testament == Testament.NEW })
    }

    @Test fun deuterocanonicalBooksAreEleven() {
        assertEquals(
            listOf("2Езд", "Тов", "Иудф", "Прем", "Сир", "ПосИер", "Вар", "1Мак", "2Мак", "3Мак", "3Езд"),
            books.filter { it.deuterocanonical }.map { it.abbr },
        )
    }

    @Test fun orthodoxOrderAroundNehemiah() {
        val abbrs = books.map { it.abbr }
        val i = abbrs.indexOf("Неем")
        assertEquals(listOf("Неем", "2Езд", "Тов", "Иудф", "Есф"), abbrs.subList(i, i + 5))
    }

    @Test fun knownChapterCounts() {
        val byAbbr = books.associateBy { it.abbr }
        assertEquals(50, byAbbr.getValue("Быт").chapters)
        assertEquals(151, byAbbr.getValue("Пс").chapters)
        assertEquals(51, byAbbr.getValue("Сир").chapters)
        assertEquals(14, byAbbr.getValue("Дан").chapters)
        assertEquals(22, byAbbr.getValue("Откр").chapters)
        assertEquals(1361, books.sumOf { it.chapters })
    }

    @Test fun abbrsAndFilesAreUnique() {
        assertEquals(77, books.map { it.abbr }.toSet().size)
        assertEquals(77, books.map { it.file }.toSet().size)
        assertTrue(books.none { it.file.contains("prayerofmanasseh") })
    }
}
