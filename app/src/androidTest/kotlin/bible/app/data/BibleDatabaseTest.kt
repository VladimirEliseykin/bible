package bible.app.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BibleDatabaseTest {
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container

    @Test fun has77BooksInOrthodoxOrder() = runTest {
        val books = container.database().books
        assertEquals(77, books.size)
        assertEquals("Быт", books.first().abbr)
        assertEquals("Откр", books.last().abbr)
        assertEquals(50, books.count { !it.newTestament })
        assertEquals("Мф", books.first { it.id == FIRST_GOSPEL_CHAPTER.bookId }.abbr)
    }

    @Test fun loadsMatthew5() = runTest {
        val verses = container.database().chapter(ChapterRef(51, 5))
        assertEquals(48, verses.size)
        assertEquals("Блаженны нищие духом, ибо их есть Царство Небесное.", verses[2].text)
    }

    @Test fun psalm118Has176Verses() = runTest {
        assertEquals(176, container.database().chapter(ChapterRef(22, 118)).size)
    }

    @Test fun chapterQueryIsFast() = runTest {
        val db = container.database()
        db.chapter(ChapterRef(1, 1))
        val t = System.nanoTime()
        db.chapter(ChapterRef(22, 118))
        assertTrue((System.nanoTime() - t) / 1_000_000 < 16)
    }
}
