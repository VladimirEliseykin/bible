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
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class VerseScreenTest {
    @get:Rule val rule = createComposeRule()
    private val inst = InstrumentationRegistry.getInstrumentation()
    private val container = (inst.targetContext.applicationContext as BibleApp).container
    private val db = runBlocking { container.database() }

    private fun installPilot() {
        val dir = File(inst.targetContext.noBackupFilesDir, "commentary").apply { mkdirs() }
        inst.context.assets.open("commentary-pilot.db").use { i -> File(dir, "commentary.db").outputStream().use { i.copyTo(it) } }
        File(dir, "commentary.version").writeText("1")
        container.pack.refresh()
    }

    private fun removePack() {
        File(inst.targetContext.noBackupFilesDir, "commentary").deleteRecursively()
        container.pack.refresh()
    }

    private fun shown(tag: String) = rule.onAllNodes(hasTestTag(tag)).fetchSemanticsNodes().isNotEmpty()
    private fun shownText(t: String) = rule.onAllNodes(hasText(t, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun open(id: Int, onBack: (Int) -> Unit = {}) {
        rule.setContent { BibleTheme { VerseScreen(db, container, id, onBack = onBack, onOpenRef = {}) } }
        rule.waitUntil(5_000) { shown("verse-glass-$id") }
    }

    @Test fun showsPillVerseAndNeighbourLabels() {
        installPilot()
        open(27002001)
        rule.onNodeWithTag("verse-pill-27002001").assertTextContains("СИРАХ 2:1")
        rule.onNodeWithTag("nav-prev-27002001").assertTextContains("1:30", substring = true)
        rule.onNodeWithTag("nav-next-27002001").assertTextContains("2:2", substring = true)
        assertTrue(shownText("Кто решил жить с Богом"))
    }

    @Test fun nextLabelOpensNextVerse() {
        installPilot()
        open(27002001)
        rule.onNodeWithTag("nav-next-27002001").performClick()
        rule.waitUntil(5_000) { shown("verse-glass-27002002") && rule.onAllNodes(hasTestTag("verse-pill-27002002")).fetchSemanticsNodes().isNotEmpty() }
    }

    @Test fun firstVerseOfBibleHasNoPrev() {
        installPilot()
        open(1001001)
        assertTrue(!shown("nav-prev-1001001"))
        assertTrue(shown("nav-next-1001001"))
    }

    @Test fun tabsSwitchAndEmptyTabsAreHidden() {
        installPilot()
        open(27002006) // у стиха 6 нет толкований отцов
        assertTrue(!shown("tab-fathers-27002006"))
        rule.onNodeWithTag("tab-refs-27002006").performClick()
        rule.waitUntil(5_000) { shownText("Предай Господу путь твой") }
        rule.onNodeWithTag("tab-translations-27002006").performClick()
        rule.waitUntil(5_000) { shownText("Вѣ́рꙋй є҆мꙋ̀") }
    }

    @Test fun bottomBarAppearsAfterScroll() {
        installPilot()
        open(27002001)
        assertTrue(!shown("verse-bottom-bar-27002001"))
        rule.onNodeWithTag("verse-list-27002001").performScrollToIndex(2)
        rule.waitUntil(5_000) { shown("verse-bottom-bar-27002001") }
    }

    @Test fun packMissingShowsDownloadCardAndSynodalText() {
        removePack()
        open(27002001)
        assertTrue(shown("pack-download-27002001"))
        assertTrue(shownText("приготовь душу твою к искушению"))
        installPilot()
    }

    @Test fun verseWithoutNoteSaysItIsComing() {
        installPilot()
        open(51005003)
        assertTrue(shown("note-missing-51005003"))
        rule.onNodeWithTag("tab-translations-51005003").performClick()
        rule.waitUntil(5_000) { shownText("бл҃же́ни") }
    }

    @Test fun longVerseAtDoubleFontDoesNotCoverHeaderButtons() {
        installPilot()
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
        installPilot()
        var back = 0
        open(27002001) { back = it }
        rule.onNodeWithTag("nav-next-27002001").performClick()
        rule.waitUntil(5_000) { shown("back-to-chapter-27002002") }
        rule.onNodeWithTag("back-to-chapter-27002002").performClick()
        rule.waitUntil(5_000) { back == 27002002 }
    }
}
