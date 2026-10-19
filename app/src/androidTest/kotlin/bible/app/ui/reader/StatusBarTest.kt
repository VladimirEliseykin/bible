package bible.app.ui.reader

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.core.view.WindowCompat
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test

class StatusBarTest {
    @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container

    private fun darkIcons(): Boolean {
        var dark = false
        rule.runOnUiThread {
            val window = rule.activity.window
            dark = WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars
        }
        return dark
    }

    @Test fun iconsAreLightOverArtAndDarkOverText() {
        val db = runBlocking { container.database() }
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(22, 118), 1)) }
        rule.setContent { BibleTheme { ReaderScreen(db, container.store) {} } }
        rule.waitUntil(5_000) { rule.onAllNodes(hasTestTag("verse-1")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitUntil(3_000) { !darkIcons() }
        rule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(30)
        rule.waitUntil(3_000) { darkIcons() }
        rule.onNodeWithTag("chapter-list-22-118").performScrollToIndex(0)
        rule.waitUntil(3_000) { !darkIcons() }
    }
}
