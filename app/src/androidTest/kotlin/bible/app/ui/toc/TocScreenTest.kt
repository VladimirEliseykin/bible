package bible.app.ui.toc

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.espresso.Espresso
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TocScreenTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    private fun shown(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    private fun openSection(group: String) {
        rule.onNodeWithTag("toc-sections").performScrollToNode(hasTestTag("section-$group"))
        rule.onNodeWithTag("section-$group").performClick()
        rule.waitUntil(5_000) { shown("section-page-$group") }
    }

    @Test fun testamentsAreHeadingsWithoutBibleTitle() {
        rule.setContent { BibleTheme { TocScreen(db, container.store) {} } }
        rule.onNodeWithText("Ветхий Завет").assertExists()
        rule.onNodeWithText("50 книг").assertExists()
        rule.onNodeWithTag("toc-sections").performScrollToNode(hasText("Новый Завет"))
        rule.onNodeWithText("27 книг").assertExists()
        assertTrue(rule.onAllNodes(hasText("Библия")).fetchSemanticsNodes().isEmpty())
    }

    @Test fun pickSectionThenBookThenChapter() {
        var opened: ChapterRef? = null
        rule.setContent { BibleTheme { TocScreen(db, container.store) { opened = it } } }
        openSection("GOSPELS")
        rule.onNodeWithTag("book-51").performClick()
        rule.onNodeWithTag("chapter-5").performClick()
        rule.waitUntil(5_000) { opened != null }
        assertEquals(ChapterRef(51, 5), opened)
    }

    @Test fun singleChapterBookOpensImmediately() {
        var opened: ChapterRef? = null
        rule.setContent { BibleTheme { TocScreen(db, container.store) { opened = it } } }
        openSection("CATHOLIC_EPISTLES")
        rule.onNodeWithTag("section-page-CATHOLIC_EPISTLES").performScrollToNode(hasTestTag("book-62"))
        rule.onNodeWithTag("book-62").performClick()
        rule.waitUntil(5_000) { opened != null }
        assertEquals(ChapterRef(62, 1), opened)
    }

    @Test fun deuterocanonicalBooksAreOnTheirPlaces() {
        rule.setContent { BibleTheme { TocScreen(db, container.store) {} } }
        openSection("WISDOM")
        rule.onNodeWithTag("section-page-WISDOM").performScrollToNode(hasText("Сирах"))
        rule.onNodeWithText("Сирах").assertExists()
    }

    @Test fun backButtonAndSystemBackReturnToSections() {
        rule.setContent { BibleTheme { TocScreen(db, container.store) {} } }
        openSection("LAW")
        rule.onNodeWithTag("back-to-sections").performClick()
        rule.waitUntil(5_000) { shown("section-LAW") && !shown("section-page-LAW") }
        openSection("LAW")
        Espresso.pressBack()
        rule.waitUntil(5_000) { shown("section-LAW") && !shown("section-page-LAW") }
    }
}
