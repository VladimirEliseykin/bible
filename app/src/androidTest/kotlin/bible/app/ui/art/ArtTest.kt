package bible.app.ui.art

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import bible.app.R
import bible.app.data.Book
import bible.app.data.BookGroup
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArtTest {
    private val ctx = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun fallsBackToGroupArt() {
        val book = Book(51, true, BookGroup.GOSPELS, "Евангелие от Матфея", "От Матфея", "Мф", 28, false)
        assertEquals(R.drawable.art_group_gospels, artFor(ctx, book))
    }

    @Test fun everyGroupHasDistinctArt() {
        assertEquals(BookGroup.entries.size, BookGroup.entries.map { artForGroup(it) }.toSet().size)
    }
}
