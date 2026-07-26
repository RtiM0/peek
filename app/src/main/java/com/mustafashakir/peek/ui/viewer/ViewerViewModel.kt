package com.mustafashakir.peek.ui.viewer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustafashakir.peek.domain.model.LinkContent
import com.mustafashakir.peek.domain.repository.LoadProgressListener
import com.mustafashakir.peek.domain.usecase.OpenLinkUseCase
import com.mustafashakir.peek.domain.usecase.RefreshLinkUseCase
import com.mustafashakir.peek.domain.usecase.LoadMoreCommentsUseCase
import com.mustafashakir.peek.ui.mapper.ViewerUiMapper
import com.mustafashakir.peek.ui.mapper.loadStageMessage
import com.mustafashakir.peek.ui.model.ViewerUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ViewerViewModel(
    private val url: String,
    private val openLink: OpenLinkUseCase,
    private val refreshLink: RefreshLinkUseCase,
    private val loadMoreComments: LoadMoreCommentsUseCase,
    private val mapper: ViewerUiMapper,
) : ViewModel() {
    private val mutableUiState = MutableStateFlow<ViewerUiState>(ViewerUiState.Loading())
    val uiState: StateFlow<ViewerUiState> = mutableUiState.asStateFlow()

    init {
        viewModelScope.launch { load { onProgress -> openLink(url, onProgress) } }
    }

    fun onRefresh() {
        mutableUiState.value = ViewerUiState.Loading()
        viewModelScope.launch { load { onProgress -> refreshLink(url, onProgress) } }
    }

    fun onLoadMoreComments() {
        val content = mutableUiState.value as? ViewerUiState.Content ?: return
        if (!content.post.canLoadMoreComments || content.isLoadingMoreComments) return
        mutableUiState.value = content.copy(isLoadingMoreComments = true)
        viewModelScope.launch {
            loadMoreComments(url).onSuccess { updated ->
                mutableUiState.value = ViewerUiState.Content(mapper.map(updated))
            }.onFailure {
                mutableUiState.value = content
            }
        }
    }

    private suspend fun load(fetch: suspend (LoadProgressListener) -> Result<LinkContent>) {
        val onProgress = LoadProgressListener { progress ->
            mutableUiState.value = ViewerUiState.Loading(progress.fraction, loadStageMessage(progress.stage))
        }
        mutableUiState.value = fetch(onProgress).fold(
            onSuccess = { ViewerUiState.Content(mapper.map(it)) },
            onFailure = { ViewerUiState.Unavailable(url) },
        )
    }

    class Factory(
        private val url: String,
        private val openLink: OpenLinkUseCase,
        private val refreshLink: RefreshLinkUseCase,
        private val loadMoreComments: LoadMoreCommentsUseCase,
        private val mapper: ViewerUiMapper,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ViewerViewModel(url, openLink, refreshLink, loadMoreComments, mapper) as T
    }
}
