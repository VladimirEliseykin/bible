package bible.app.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class RussianPluralTest {
    @Test fun chapters() {
        assertEquals("1 глава", chaptersLabel(1))
        assertEquals("21 глава", chaptersLabel(21))
        assertEquals("2 главы", chaptersLabel(2))
        assertEquals("24 главы", chaptersLabel(24))
        assertEquals("5 глав", chaptersLabel(5))
        assertEquals("11 глав", chaptersLabel(11))
        assertEquals("12 глав", chaptersLabel(12))
        assertEquals("151 глава", chaptersLabel(151))
    }

    @Test fun books() {
        assertEquals("1 книга", booksLabel(1))
        assertEquals("4 книги", booksLabel(4))
        assertEquals("15 книг", booksLabel(15))
    }
}
