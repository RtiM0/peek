package com.mustafashakir.peek.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.mustafashakir.peek.domain.usecase.ObserveRecentContentUseCase
import com.mustafashakir.peek.ui.mapper.HomeUiMapper
import com.mustafashakir.peek.ui.model.HomeUiState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(
    observeRecentContent: ObserveRecentContentUseCase,
    mapper: HomeUiMapper,
) : ViewModel() {
    val uiState: StateFlow<HomeUiState> = observeRecentContent()
        .map(mapper::map)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState.Loading,
        )

    class Factory(
        private val observeRecentContent: ObserveRecentContentUseCase,
        private val mapper: HomeUiMapper,
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            HomeViewModel(observeRecentContent, mapper) as T
    }
}
