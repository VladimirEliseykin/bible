package bible.app.ui.toc

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TocScreenTest {
    @get:Rule val rule = createComposeRule()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    @Test fun pickBookThenChapter() {
        var opened: ChapterRef? = null
        rule.setContent { BibleTheme { TocScreen(db, container.store) { opened = it } } }
        rule.onNodeWithTag("toc-list").performScrollToNode(hasTestTag("shelf-GOSPELS"))
        rule.onNodeWithTag("book-51").performClick()
        rule.onNodeWithTag("chapter-5").performClick()
        rule.waitUntil(5_000) { opened != null }
        assertEquals(ChapterRef(51, 5), opened)
    }

    @Test fun singleChapterBookOpensImmediately() {
        var opened: ChapterRef? = null
        rule.setContent { BibleTheme { TocScreen(db, container.store) { opened = it } } }
        rule.onNodeWithTag("toc-list").performScrollToNode(hasTestTag("shelf-CATHOLIC_EPISTLES"))
        rule.onNodeWithTag("shelf-CATHOLIC_EPISTLES").performScrollToNode(hasTestTag("book-62"))
        rule.onNodeWithTag("book-62").performClick()
        rule.waitUntil(5_000) { opened != null }
        assertEquals(ChapterRef(62, 1), opened)
    }

    @Test fun deuterocanonicalBooksAreOnTheirPlaces() {
        rule.setContent { BibleTheme { TocScreen(db, container.store) {} } }
        rule.onNodeWithTag("toc-list").performScrollToNode(hasTestTag("shelf-WISDOM"))
        rule.onNodeWithTag("shelf-WISDOM").performScrollToNode(hasText("Сирах"))
        rule.onNodeWithText("Сирах").assertExists()
    }
}
