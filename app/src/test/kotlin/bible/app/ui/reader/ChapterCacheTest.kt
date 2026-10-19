package bible.app.ui.reader

import bible.app.data.ChapterRef
import bible.app.data.Verse
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChapterCacheTest {
    private val verses = listOf(Verse(1, 1, "Текст"))

    @Test fun loadsChapter() = runTest(UnconfinedTestDispatcher()) {
        val cache = ChapterCache(backgroundScope) { verses }
        assertEquals(ChapterContent.Loaded(verses), cache.chapter(ChapterRef(1, 1)).value)
    }

    @Test fun failureIsReportedAndRetryLoads() = runTest(UnconfinedTestDispatcher()) {
        var fail = true
        val cache = ChapterCache(backgroundScope) { if (fail) throw IOException("disk") else verses }
        val flow = cache.chapter(ChapterRef(1, 1))
        assertEquals(ChapterContent.Failed, flow.value)
        fail = false
        cache.retry(ChapterRef(1, 1))
        assertEquals(ChapterContent.Loaded(verses), flow.value)
    }

    @Test fun keepsEnoughChaptersForVisiblePagesAndNeighbours() = runTest(UnconfinedTestDispatcher()) {
        var loads = 0
        val cache = ChapterCache(backgroundScope) { loads++; verses }
        val first = cache.chapter(ChapterRef(1, 1))
        (2..7).forEach { cache.chapter(ChapterRef(1, it)) }
        assertSame(first, cache.chapter(ChapterRef(1, 1)))
        assertEquals(7, loads)
    }
}
