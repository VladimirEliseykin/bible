package bible.app.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import bible.app.MainActivity
import org.junit.Rule
import org.junit.Test

class AppRootTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun tabsSwitchBetweenHomeAndBible() {
        rule.waitUntil(10_000) { rule.onAllNodes(hasTestTag("screen-home")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithText("Библия").performClick()
        rule.onNodeWithTag("screen-reader").assertIsDisplayed()
        rule.onNodeWithText("Главная").performClick()
        rule.onNodeWithTag("screen-home").assertIsDisplayed()
    }
}
