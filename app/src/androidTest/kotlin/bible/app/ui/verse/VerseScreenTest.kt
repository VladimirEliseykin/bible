package bible.app.ui.verse

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VerseScreenTest {
    @get:Rule val rule = createComposeRule()
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val container = (inst.targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }
    private val books = db.books.associateBy { it.id }
    private fun data(id: Int) = runBlocking { loadVerse(db, books, container.commentary(), id) }

    private fun shown(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun shownText(t: String) = rule.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun open(id: Int, onBack: (Int) -> Unit = {}) {
        rule.setContent { BibleTheme { VerseScreen(db, container, id, onBack = onBack, onOpenRef = {}) } }
        rule.waitUntil(5_000) { shown("verse-glass-$id") }
    }

    @Test fun showsPillVerseAndNeighbourLabels() {
        open(27002001)
        rule.onNodeWithTag("verse-pill-27002001").assertTextContains("СИРАХ 2:1")
        rule.onNodeWithTag("nav-prev-27002001").assertTextContains("1:30", substring = true)
        rule.onNodeWithTag("nav-next-27002001").assertTextContains("2:2", substring = true)
        assertTrue(shownText(data(27002001).note!!.lead.take(30)))
    }

    @Test fun nextLabelOpensNextVerse() {
        open(27002001)
        rule.onNodeWithTag("nav-next-27002001").performClick()
        rule.waitUntil(5_000) { shown("verse-glass-27002002") && rule.onAllNodes(hasTestTag("verse-pill-27002002")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun firstVerseOfBibleHasNoPrev() {
        open(1001001)
        assertTrue(!shown("nav-prev-1001001"))
        assertTrue(shown("nav-next-1001001"))
    }

    @Test fun tabsMatchDataAndSwitch() {
        val d = data(27002001)
        val tabs = d.tabs()
        assertTrue("у пилотного стиха есть смысл и толкования", VerseTab.Meaning in tabs && VerseTab.Fathers in tabs)
        open(27002001)
        VerseTab.entries.forEach { t -> assertEquals(t.name, t in tabs, shown("tab-${t.tag}-27002001")) }
        assertTrue(shownText(d.meaning.first().title))
        rule.onNodeWithTag("tab-fathers-27002001").performClick()
        rule.waitUntil(5_000) { shownText(d.fathers.first().author) }
    }

    @Test fun bottomBarAppearsAfterScroll() {
        open(27002001)
        assertTrue(!shown("verse-bottom-bar-27002001"))
        rule.onNodeWithTag("verse-list-27002001").performScrollToIndex(2)
        rule.waitUntil(5_000) { shown("verse-bottom-bar-27002001") }
    }

    @Test fun verseWithoutNoteSaysItIsComingAndHasNoTabs() {
        open(1001001)
        assertTrue(shown("note-missing-1001001"))
        VerseTab.entries.forEach { assertTrue(!shown("tab-${it.tag}-1001001")) }
    }

    @Test fun longVerseAtDoubleFontDoesNotCoverHeaderButtons() {
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                BibleTheme { VerseScreen(db, container, 12002012, onBack = {}, onOpenRef = {}) }
            }
        }
        rule.waitUntil(5_000) { shown("verse-glass-12002012") }
        val back = rule.onNodeWithTag("back-to-chapter-12002012").fetchSemanticsNode().boundsInRoot
        val glass = rule.onNodeWithTag("verse-glass-12002012").fetchSemanticsNode().boundsInRoot
        assertTrue("плашка $glass заходит на кнопку $back", glass.top >= back.bottom)
    }

    @Test fun backReturnsCurrentVerse() {
        var back = 0
        open(27002001) { back = it }
        rule.onNodeWithTag("nav-next-27002001").performClick()
        rule.waitUntil(5_000) { shown("back-to-chapter-27002002") }
        rule.onNodeWithTag("back-to-chapter-27002002").performClick()
        rule.waitUntil(5_000) { back == 27002002 }
    }
}
