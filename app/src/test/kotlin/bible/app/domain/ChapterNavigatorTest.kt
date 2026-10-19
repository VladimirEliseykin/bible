package bible.app.domain

import bible.app.data.Book
import bible.app.data.BookGroup
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChapterNavigatorTest {
    private fun book(id: Int, chapters: Int) = Book(id, false, BookGroup.LAW, "Книга $id", "К$id", "К$id", chapters, false)
    private val nav = ChapterNavigator(listOf(book(1, 3), book(2, 1), book(3, 2)))

    @Test fun nextWithinBook() = assertEquals(ChapterRef(1, 2), nav.next(ChapterRef(1, 1)))
    @Test fun nextCrossesIntoNextBook() = assertEquals(ChapterRef(2, 1), nav.next(ChapterRef(1, 3)))
    @Test fun nextOverSingleChapterBook() = assertEquals(ChapterRef(3, 1), nav.next(ChapterRef(2, 1)))
    @Test fun nextAtEndOfBibleIsNull() = assertNull(nav.next(ChapterRef(3, 2)))
    @Test fun previousCrossesToLastChapterOfPreviousBook() = assertEquals(ChapterRef(1, 3), nav.previous(ChapterRef(2, 1)))
    @Test fun previousAtStartOfBibleIsNull() = assertNull(nav.previous(ChapterRef(1, 1)))

    @Test fun indexRoundTrip() {
        assertEquals(6, nav.totalChapters)
        (0 until 6).forEach { assertEquals(it, nav.indexOf(nav.refAt(it))) }
        assertEquals(ChapterRef(3, 1), nav.refAt(4))
    }

    @Test fun sanitizeKeepsValid() = assertEquals(ReadingPosition(ChapterRef(1, 2), 7), nav.sanitize(ReadingPosition(ChapterRef(1, 2), 7)))
    @Test fun sanitizeUnknownBookGoesToStartWhenNoGospel() = assertEquals(ReadingPosition(ChapterRef(1, 1), 1), nav.sanitize(ReadingPosition(ChapterRef(99, 5), 3)))
    @Test fun sanitizeChapterTooBigGoesToLastChapter() = assertEquals(ReadingPosition(ChapterRef(3, 2), 1), nav.sanitize(ReadingPosition(ChapterRef(3, 9), 3)))
    @Test fun sanitizeZeroChapterAndVerse() = assertEquals(ReadingPosition(ChapterRef(1, 1), 1), nav.sanitize(ReadingPosition(ChapterRef(1, 0), 0)))

    @Test fun sanitizeUnknownBookGoesToMatthew1() {
        val withGospel = ChapterNavigator(listOf(book(1, 3), book(51, 28)))
        assertEquals(ReadingPosition(ChapterRef(51, 1), 1), withGospel.sanitize(ReadingPosition(ChapterRef(99, 5), 3)))
    }

    @Test fun bookLookupById() = assertEquals(2, nav.book(2).id)
}
