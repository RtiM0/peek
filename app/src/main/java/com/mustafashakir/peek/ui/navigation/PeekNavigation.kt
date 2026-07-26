package com.mustafashakir.peek.ui.navigation

import androidx.activity.BackEventCompat
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.mustafashakir.peek.app.AppContainer
import com.mustafashakir.peek.ui.home.HomeRoute
import com.mustafashakir.peek.ui.home.HomeViewModel
import com.mustafashakir.peek.ui.player.PlayerRoute
import com.mustafashakir.peek.ui.player.PlayerViewModel
import com.mustafashakir.peek.ui.viewer.ViewerRoute
import com.mustafashakir.peek.ui.viewer.ViewerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeekNavigation(container: AppContainer, viewIntentUrl: MutableState<String?>) {
    val backStack = rememberNavBackStack(HomeKey)
    val homeViewModel: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(container.observeRecentContent, container.homeUiMapper),
    )
    val homeUiState by homeViewModel.uiState.collectAsStateWithLifecycle()

    fun regularPop() {
        if (backStack.size > 1) backStack.removeLastOrNull()
    }

    LaunchedEffect(viewIntentUrl.value) {
        val url = viewIntentUrl.value ?: return@LaunchedEffect
        viewIntentUrl.value = null
        backStack.add(ViewerKey(url))
    }

    NavDisplay(
        backStack = backStack,
        onBack = ::regularPop,
        transitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        popTransitionSpec = {
            EnterTransition.None togetherWith ExitTransition.None
        },
        predictivePopTransitionSpec = { swipeEdge ->
            val direction = if (swipeEdge == BackEventCompat.EDGE_LEFT) 1 else -1
            slideInHorizontally(initialOffsetX = { -it * direction }) togetherWith
                slideOutHorizontally(targetOffsetX = { it * direction })
        },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<HomeKey> {
                HomeRoute(
                    uiState = homeUiState,
                    onOpenLink = { url -> backStack.add(ViewerKey(url)) },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
            entry<ViewerKey> { key ->
                val viewerViewModel: ViewerViewModel = viewModel(
                    factory = ViewerViewModel.Factory(key.url, container.openLink, container.refreshLink, container.loadMoreComments, container.viewerUiMapper),
                )
                ViewerRoute(
                    viewModel = viewerViewModel,
                    onBack = ::regularPop,
                    onOpenMedia = { mediaIndex -> backStack.add(PlayerKey(key.url, mediaIndex)) },
                    modifier = Modifier.safeDrawingPadding(),
                )
            }
            entry<PlayerKey> { key ->
                val playerViewModel: PlayerViewModel = viewModel(
                    factory = PlayerViewModel.Factory(key.url, container.openLink, container.loadMoreComments, container.viewerUiMapper),
                )
                PlayerRoute(
                    viewModel = playerViewModel,
                    initialMediaIndex = key.mediaIndex,
                    onBack = ::regularPop,
                )
            }
        },
    )
}
