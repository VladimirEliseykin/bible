package bible.app.ui.reader

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.Lifecycle
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PositionRestoreTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val createAndroidComposeRuleForStop get() = rule
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    private fun saved() = runBlocking { container.store.position.first() }

    private fun waitSaved(expected: ReadingPosition) = rule.waitUntil(5_000) { saved() == expected }

    @Test fun swipingAwayAndBackKeepsVerseOfChapter() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(51, 5), 20)) }
        rule.setContent { BibleTheme { ReaderScreen(db, container.store) {} } }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-20")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        waitSaved(ReadingPosition(ChapterRef(51, 6), 1))
        rule.onNodeWithTag("pager").performTouchInput { swipeRight() }
        waitSaved(ReadingPosition(ChapterRef(51, 5), 20))
    }

    @Test fun recreationKeepsCurrentVerseNotStartingVerse() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(51, 5), 20)) }
        val tester = StateRestorationTester(rule)
        tester.setContent { BibleTheme { ReaderScreen(db, container.store) {} } }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-20")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("chapter-list-51-5").performScrollToIndex(40)
        waitSaved(ReadingPosition(ChapterRef(51, 5), 40))
        tester.emulateSavedInstanceStateRestore()
        rule.waitForIdle()
        Thread.sleep(800)
        assertEquals(ReadingPosition(ChapterRef(51, 5), 40), saved())
    }

    @Test fun goingToBackgroundRightAfterScrollSavesVerse() {
        val activityRule = createAndroidComposeRuleForStop
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(22, 118), 1)) }
        activityRule.setContent { BibleTheme { ReaderScreen(db, container.store) {} } }
        activityRule.waitUntil(5_000) { activityRule.onAllNodes(hasTestTag("verse-1")).fetchSemanticsNodes().isNotEmpty() }
        activityRule.mainClock.autoAdvance = false
        activityRule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(77)
        activityRule.mainClock.advanceTimeBy(50)
        activityRule.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        activityRule.mainClock.autoAdvance = true
        activityRule.waitUntil(5_000) { saved() == ReadingPosition(ChapterRef(22, 118), 77) }
    }
}
