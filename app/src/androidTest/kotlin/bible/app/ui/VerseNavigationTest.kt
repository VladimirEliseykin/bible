package bible.app.ui

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import bible.app.MainActivity
import bible.app.data.ChapterRef
import bible.app.data.ReadingPosition
import bible.app.ui.verse.loadVerse
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

    @Test fun refCardInMeaningOpensThatPlaceInReader() {
        val db = runBlocking { container.database() }
        val books = db.books.associateBy { it.id }
        val (id, ref) = runBlocking {
            (27002001..27002018).firstNotNullOf { id -> loadVerse(db, books, container.commentary(), id).meaning.flatMap { it.refs }.firstOrNull()?.let { id to it } }
        }
        runBlocking { container.store.savePosition(ReadingPosition(ChapterRef(27, 2), id % 1_000)) }
        rule.waitUntil(10_000) { shown("tab-bible") }
        rule.onNodeWithTag("tab-bible").performClick()
        rule.waitUntil(5_000) { shown("verse-${id % 1_000}") }
        rule.onNode(hasTestTag("verse-${id % 1_000}") and hasAnyAncestor(hasTestTag("chapter-list-27-2"))).performClick()
        rule.waitUntil(5_000) { shown("tab-meaning-$id") }
        rule.onNode(hasText(ref.label) and hasAnyAncestor(hasTestTag("verse-list-$id"))).performScrollTo().performClick()
        val ch = ref.verseId / 1_000 % 1_000
        rule.waitUntil(5_000) { shown("screen-reader") && shown("chapter-list-${ref.verseId / 1_000_000}-$ch") }
        assertEquals(ChapterRef(ref.verseId / 1_000_000, ch), runBlocking { container.store.position.first()!!.ref })
    }
}
