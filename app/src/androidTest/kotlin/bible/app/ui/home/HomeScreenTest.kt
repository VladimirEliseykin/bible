package bible.app.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.data.ChapterRef
import bible.app.data.FIRST_GOSPEL_CHAPTER
import bible.app.data.ReadingPosition
import bible.app.ui.theme.BibleTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {
    @get:Rule val rule = createComposeRule()
    private val container = (InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    @Test fun showsSavedPositionAndOpensIt() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(22, 22), 3)) }
        var opened = false
        rule.setContent { BibleTheme { HomeScreen(db, container.store) { opened = true } } }
        rule.onNodeWithTag("carousel").assertExists()
        rule.onNodeWithText("Псалтирь 22").performClick()
        rule.waitForIdle()
        assertTrue(opened)
    }

    @Test fun invalidSavedPositionIsSanitized() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(99, 9), 9)) }
        rule.setContent { BibleTheme { HomeScreen(db, container.store) {} } }
        rule.onNodeWithText("Бытие 1").assertExists()
    }

    @Test fun startButtonSavesMatthew1() {
        runBlocking { container.store.clearPosition() }
        var opened = false
        rule.setContent { BibleTheme { HomeScreen(db, container.store) { opened = true } } }
        rule.onNodeWithText("Начать с Евангелия от Матфея").performClick()
        rule.waitForIdle()
        assertEquals(FIRST_GOSPEL_CHAPTER, runBlocking { container.store.position.first()!!.ref })
        assertTrue(opened)
    }
}
