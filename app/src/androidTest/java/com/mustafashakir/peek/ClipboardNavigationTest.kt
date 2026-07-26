package com.mustafashakir.peek

import android.content.ClipData
import android.content.ClipboardManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import org.junit.Rule
import org.junit.Test

class ClipboardNavigationTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun copiedWebUrlNavigatesToViewer() {
        setClipboard("https://www.instagram.com/reel/DapVyootsZw/")

        composeRule.onNodeWithText("Paste from clipboard").assertIsDisplayed().performClick()

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed()
    }

    @Test
    fun textWithoutWebUrlShowsFeedbackAndStaysHome() {
        setClipboard("This clipboard has no link")

        composeRule.onNodeWithText("Paste from clipboard").performClick()

        composeRule.onNodeWithText("Copy a valid web link, then try again").assertIsDisplayed()
        composeRule.onNodeWithText("Paste from clipboard").assertIsDisplayed()
    }

    private fun setClipboard(text: String) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Peek test link", text))
    }
}
