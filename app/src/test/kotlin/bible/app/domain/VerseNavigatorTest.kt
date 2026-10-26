package bible.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VerseNavigatorTest {
    // книга 1: глава 1 (стихи 1–2), глава 2 (стих 1); книга 2: глава 1 (стихи 1–3)
    private val nav = VerseNavigator(intArrayOf(1001001, 1001002, 1002001, 2001001, 2001002, 2001003))

    @Test fun nextInsideChapter() = assertEquals(1001002, nav.next(1001001))
    @Test fun nextAcrossChapter() = assertEquals(1002001, nav.next(1001002))
    @Test fun prevAcrossBook() = assertEquals(1002001, nav.prev(2001001))
    @Test fun firstHasNoPrev() = assertNull(nav.prev(1001001))
    @Test fun lastHasNoNext() = assertNull(nav.next(2001003))
    @Test fun indexRoundTrip() {
        assertEquals(6, nav.size)
        (0 until 6).forEach { assertEquals(it, nav.indexOf(nav.idAt(it))) }
    }
    @Test fun labelIsChapterAndVerse() = assertEquals("1:30", VerseNavigator.label(27001030))
    @Test fun positionInChapter() = assertEquals(2 to 3, nav.positionInChapter(2001002))
}
