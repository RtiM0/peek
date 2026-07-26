package com.mustafashakir.peek

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import com.mustafashakir.peek.ui.home.HomeView
import com.mustafashakir.peek.ui.model.ViewerUiState
import com.mustafashakir.peek.ui.player.PlayerView
import com.mustafashakir.peek.ui.preview.PeekPreviewFixtures
import com.mustafashakir.peek.ui.theme.PeekTheme
import com.mustafashakir.peek.ui.viewer.ViewerView
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class PureViewTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeRendersRepositoryStateAndForwardsSelection() {
        var openedUrl: String? = null
        composeRule.setContent {
            PeekTheme {
                HomeView(
                    uiState = PeekPreviewFixtures.home,
                    onPasteClick = {},
                    onRecentLink = { openedUrl = it },
                )
            }
        }

        composeRule.onNodeWithText("A quiet morning in Kyoto").assertIsDisplayed().performClick()
        assertEquals("preview://kyoto", openedUrl)
    }

    @Test
    fun homeForwardsPasteFromEmptyState() {
        var pasteClicked = false
        composeRule.setContent {
            PeekTheme {
                HomeView(
                    uiState = com.mustafashakir.peek.ui.model.HomeUiState.Empty,
                    onPasteClick = { pasteClicked = true },
                    onRecentLink = {},
                )
            }
        }

        composeRule.onNodeWithText("Paste from clipboard").assertIsDisplayed().performClick()
        assertEquals(true, pasteClicked)
    }

    @Test
    fun viewerRendersStateAndForwardsBack() {
        var backPressed = false
        composeRule.setContent {
            PeekTheme {
                ViewerView(
                    uiState = PeekPreviewFixtures.video,
                    onBack = { backPressed = true },
                    onRefresh = {},
                    onOpenMedia = {},
                )
            }
        }

        composeRule.onNodeWithText("Mara Chen").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Back").performClick()
        assertEquals(true, backPressed)
    }

    @Test
    fun viewerOpensPhotoAtItsCarouselIndex() {
        var openedIndex: Int? = null
        composeRule.setContent {
            PeekTheme {
                ViewerView(
                    uiState = PeekPreviewFixtures.carousel,
                    onBack = {},
                    onRefresh = {},
                    onOpenMedia = { openedIndex = it },
                )
            }
        }

        composeRule.onNodeWithContentDescription("Kyoto photo").performClick()
        assertEquals(1, openedIndex)
    }

    @Test
    fun fullScreenCarouselStartsAtSelectedPhoto() {
        composeRule.setContent {
            PeekTheme {
                PlayerView(
                    uiState = PeekPreviewFixtures.carousel,
                    initialMediaIndex = 1,
                    onBack = {},
                    onMore = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Kyoto photo").assertIsDisplayed()
        composeRule.onNodeWithText("2 / 3").assertIsDisplayed()
        composeRule.onNodeWithText("PHOTO").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Kyoto photo").performTouchInput {
            swipeLeft()
        }
        composeRule.onNodeWithText("3 / 3").assertIsDisplayed()
        composeRule.onAllNodesWithContentDescription("Play video").assertCountEquals(2)
    }

    @Test
    fun loadingViewerShowsBackWithoutViewerChrome() {
        var backPressed = false
        composeRule.setContent {
            PeekTheme {
                ViewerView(ViewerUiState.Loading(), onBack = { backPressed = true }, onRefresh = {}, onOpenMedia = {})
            }
        }

        composeRule.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("VIEWING POST").assertDoesNotExist()
        composeRule.onNodeWithText("VIDEO PREVIEW").assertDoesNotExist()
        assertEquals(true, backPressed)
    }

    @Test
    fun loadingViewerShowsCancelThatTriggersBack() {
        var backPressed = false
        composeRule.setContent {
            PeekTheme {
                ViewerView(ViewerUiState.Loading(0.4f, "Fetching the page"), onBack = { backPressed = true }, onRefresh = {}, onOpenMedia = {})
            }
        }

        composeRule.onNodeWithText("Cancel").assertIsDisplayed().performClick()
        assertEquals(true, backPressed)
    }
}
