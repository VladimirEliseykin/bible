package bible.app.ui

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.MainActivity
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class VerseNavigationTest {
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val container = (inst.targetContext.applicationContext as BibleApp).container

    @Before fun setUp() {
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(27, 2), 1)) }
    }

    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    private fun shown(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()

    @Test fun tapVerseOpensPageAndBackReturnsToVerseWhereYouStopped() {
        rule.waitUntil(10_000) { shown("tab-bible") }
        rule.onNodeWithTag("tab-bible").performClick()
        rule.waitUntil(5_000) { shown("verse-1") }
        rule.onNode(hasTestTag("verse-1") and hasAnyAncestor(hasTestTag("chapter-list-27-2"))).performClick()
        rule.waitUntil(5_000) { shown("verse-glass-27002001") }
        assertEquals(false, shown("bottom-tabs"))
        rule.onNodeWithTag("nav-next-27002001").performClick()
        rule.waitUntil(5_000) { shown("back-to-chapter-27002002") }
        rule.onNodeWithTag("back-to-chapter-27002002").performClick()
        rule.waitUntil(5_000) { shown("screen-reader") && shown("verse-2") }
        assertEquals(ReadingPosition(ChapterRef(27, 2), 2), runBlocking { container.store.position.first() })
    }

    @Test fun refCardOpensThatPlaceInReader() {
        rule.waitUntil(10_000) { shown("tab-bible") }
        rule.onNodeWithTag("tab-bible").performClick()
        rule.waitUntil(5_000) { shown("verse-18") || shown("verse-1") }
        rule.onNode(hasTestTag("verse-1") and hasAnyAncestor(hasTestTag("chapter-list-27-2"))).performClick()
        rule.waitUntil(5_000) { shown("tab-refs-27002001") }
        rule.onNodeWithTag("tab-refs-27002001").performClick()
        rule.waitUntil(5_000) { rule.onAllNodes(androidx.compose.ui.test.hasText("Иакова 1:2–3")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(androidx.compose.ui.test.hasText("Иакова 1:2–3")).performClick()
        rule.waitUntil(5_000) { shown("screen-reader") && shown("chapter-list-56-1") }
        assertEquals(ChapterRef(56, 1), runBlocking { container.store.position.first()!!.ref })
    }
}
