package bible.app.ui.reader

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class LargeFontReaderTest {
    @get:Rule val rule = createComposeRule()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container

    @After fun restore() = runBlocking { container.store.setTextSize(2) }

    @Test fun longestChapterAtDoubleFontScrollsToEnd() {
        val db = runBlocking { container.database() }
        runBlocking {
            container.store.savePosition(ReadingPosition(ChapterRef(22, 118), 1))
            container.store.setTextSize(4)
        }
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                BibleTheme { ReaderScreen(db, container.store) {} }
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-1")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(176)
        rule.onNodeWithTag("verse-176").assertExists()
        val three = rule.onNode(hasTestTag("verse-number-176") and hasAnyAncestor(hasTestTag("chapter-list-22-118"))).fetchSemanticsNode().size.height
        rule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(1)
        val one = rule.onNode(hasTestTag("verse-number-1") and hasAnyAncestor(hasTestTag("chapter-list-22-118"))).fetchSemanticsNode().size.height
        assertEquals("номер 176 должен помещаться в одну строку", one, three)
    }

    @Test fun longTitleKeepsHeaderButtonsOnScreenAtDoubleFont() {
        val db = runBlocking { container.database() }
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(26, 19), 1)) } // Премудрость Соломона 19
        rule.setContent {
            val d = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(d.density, fontScale = 2f)) {
                BibleTheme { ReaderScreen(db, container.store) {} }
            }
        }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-1")).fetchSemanticsNodes().isNotEmpty() }
        val width = rule.onRoot().fetchSemanticsNode().size.width
        listOf("text-size", "chapter-title", "prev-chapter", "next-chapter").forEach { tag ->
            val bounds = rule.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
            assertTrue("$tag выходит за экран: $bounds", bounds.left >= 0f && bounds.right <= width.toFloat())
            assertTrue("$tag перекрыт", bounds.width > 0f)
        }
        val size = rule.onNodeWithTag("next-chapter").fetchSemanticsNode().size
        val glyph = rule.onNodeWithTag("next-chapter-glyph", useUnmergedTree = true).fetchSemanticsNode().size
        assertTrue("стрелка обрезана: $glyph в $size", glyph.height <= size.height)
    }
}
