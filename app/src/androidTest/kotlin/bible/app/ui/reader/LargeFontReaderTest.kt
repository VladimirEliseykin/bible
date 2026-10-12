package bible.app.ui.reader

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.unit.Density
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.After
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
    }
}
