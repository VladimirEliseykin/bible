package bible.app.ui

import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.MainActivity
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ImmersiveReadingTest {
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container

    @Before fun position() = runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(51, 5), 1)) }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shown(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun openReader() {
        rule.waitUntil(10_000) { shown("tab-bible") }
        rule.onNodeWithTag("tab-bible").performClick()
        rule.waitUntil(5_000) { shown("verse-1") }
    }

    @Test fun scrollingDownHidesEverythingEndShowsOnlyChapterBarScrollUpShowsAll() {
        openReader()
        assertTrue(shown("bottom-tabs") && shown("chapter-bar") && shown("chapter-title"))
        rule.onNodeWithTag("chapter-list-51-5").performTouchInput { swipeUp(startY = centerY + 200f, endY = centerY - 100f) }
        rule.waitUntil(3_000) { !shown("bottom-tabs") && !shown("chapter-bar") && !shown("chapter-title") }
        rule.onNodeWithTag("chapter-list-51-5").performScrollToIndex(48)
        rule.onNodeWithTag("chapter-list-51-5").performTouchInput { swipeUp() }
        rule.waitUntil(3_000) { shown("chapter-bar") }
        assertTrue("шапка не должна появляться в конце", !shown("chapter-title"))
        assertTrue("вкладки не должны появляться в конце", !shown("bottom-tabs"))
        rule.onNodeWithTag("chapter-list-51-5").performTouchInput { swipeDown(startY = centerY - 200f, endY = centerY + 100f) }
        rule.waitUntil(3_000) { shown("bottom-tabs") && shown("chapter-title") }
    }

    @Test fun bottomTabsAreCompact() {
        rule.waitUntil(10_000) { shown("tab-bible") }
        val height = rule.onNodeWithTag("bottom-tabs-content").fetchSemanticsNode().size.height
        val maxPx = with(rule.density) { 60.dp.roundToPx() }
        assertTrue("вкладки $height px > $maxPx", height <= maxPx)
    }

    @Test fun tocOpenedFromReaderReturnsToSameVerse() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(51, 5), 20)) }
        openReader()
        rule.waitUntil(5_000) { shown("verse-20") }
        rule.onNodeWithTag("chapter-title").performClick()
        rule.waitUntil(5_000) { shown("back-to-reading") }
        rule.onNodeWithText("От Матфея 5", substring = true).assertExists()
        rule.onNodeWithTag("back-to-reading").performClick()
        rule.waitUntil(5_000) { shown("verse-20") && shown("screen-reader") }
        assertEquals(ReadingPosition(ChapterRef(51, 5), 20), runBlocking { container.store.position.first() })
    }
}
