package bible.app.ui.reader

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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

class ReaderScreenTest {
    @get:Rule val rule = createComposeRule()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    private fun open(ref: ChapterRef, verse: Int = 1) {
        runBlocking { container.store.savePosition(ReadingPosition(ref, verse)) }
        rule.setContent { BibleTheme { ReaderScreen(db, container.store) {} } }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("chapter-title")).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun waitForTitle(text: String) =
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("chapter-title") and hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    @Test fun showsChapterTitleAndVerses() {
        open(ChapterRef(51, 5))
        waitForTitle("От Матфея 5")
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("Блаженны нищие духом, ибо их есть Царство Небесное.")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun swipeLeftFromLastChapterGoesToNextBook() {
        open(ChapterRef(51, 28))
        rule.onNodeWithTag("pager").performTouchInput { swipeLeft() }
        waitForTitle("От Марка 1")
    }

    @Test fun swipeRightOnGenesis1DoesNothing() {
        open(ChapterRef(1, 1))
        rule.onNodeWithTag("pager").performTouchInput { swipeRight() }
        rule.waitForIdle()
        waitForTitle("Бытие 1")
    }

    @Test fun nextButtonOpensNextChapter() {
        open(ChapterRef(51, 5))
        rule.onNodeWithTag("next-chapter").performClick()
        waitForTitle("От Матфея 6")
    }

    @Test fun scrollPositionIsSaved() {
        open(ChapterRef(22, 118))
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-1")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(100)
        rule.waitUntil(5_000) {
            runBlocking { container.store.position.first() } == ReadingPosition(ChapterRef(22, 118), 100)
        }
    }

    @Test fun reopensOnSavedVerse() {
        open(ChapterRef(22, 118), verse = 150)
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-150")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun openingChapterDoesNotOverwriteSavedVerse() {
        open(ChapterRef(22, 118), verse = 150)
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-150")).fetchSemanticsNodes().isNotEmpty() }
        Thread.sleep(800)
        assertEquals(150, runBlocking { container.store.position.first()!!.verse })
    }

    @Test fun chapterBarShowsGoldPillTitle() {
        open(ChapterRef(51, 5))
        rule.onNodeWithTag("chapter-bar-title").assertTextEquals("ОТ МАТФЕЯ 5")
    }
}
