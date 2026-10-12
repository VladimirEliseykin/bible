package bible.app.ui.reader

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import bible.app.ui.theme.BibleTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class TextSizeSheetTest {
    @get:Rule val rule = createComposeRule()

    @Test fun stepsAreClampedAndReported() {
        var size = 4
        var spacing = 1
        rule.setContent { BibleTheme { TextSizeSheet(size, spacing, { size = it }, { spacing = it }) {} } }
        rule.onNodeWithTag("text-larger").performClick()
        assertEquals(4, size)
        rule.onNodeWithTag("text-smaller").performClick()
        assertEquals(3, size)
        rule.onNodeWithTag("spacing-2").performClick()
        assertEquals(2, spacing)
    }
}
