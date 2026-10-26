package bible.app.commentary

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CommentaryDatabaseTest {
    /** Та же база, что лежит в APK. */
    private val db: CommentaryDatabase by lazy {
        val app = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as BibleApp
        runBlocking { app.container.commentary() }
    }

    @Test fun everyPilotVerseHasLeadAndMeaning() {
        val ids = (27002001..27002018) + (51005001..51005012)
        ids.forEach { id ->
            val n = db.note(id)
            assertTrue("$id", n != null && n.lead.isNotBlank() && n.meaning.isNotEmpty())
        }
    }

    @Test fun chapterNoteAndSources() {
        assertTrue(db.chapter(51, 5)!!.context.isNotEmpty())
        assertTrue(db.sourceUrl("lopukhin")!!.startsWith("https://"))
    }

    @Test fun missingVerseNoteIsNull() = assertNull(db.note(1001001))
}
