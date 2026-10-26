package bible.app.commentary

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.BibleApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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

    @Test fun readsPilotSirach2() {
        val n = db.note(27002001)!!
        assertTrue(n.lead.startsWith("Кто решил жить с Богом"))
        assertEquals(56001002, n.refs.first().verse_id)
        assertEquals("Кто и когда", db.chapter(27, 2)!!.context.first().title)
    }

    @Test fun churchSlavonicTranslation() = assertTrue(db.translation(27002001, "csl")!!.startsWith("Ча́до"))

    @Test fun missingVerseNoteIsNull() = assertNull(db.note(51005003))
}
