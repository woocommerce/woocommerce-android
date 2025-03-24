package com.woocommerce.android.ui.woopos.home.items.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooPosItemsSearchViewModel @Inject constructor(
    val emptyStateProvider: WooPosItemsSearchEmptyStateProvider,
) : ViewModel() {
    private val _viewState =
        MutableStateFlow<WooPosItemsSearchViewState>(WooPosItemsSearchViewState.Empty)
    val viewState: StateFlow<WooPosItemsSearchViewState> = _viewState
        .stateIn(
            viewModelScope,
            started = SharingStarted.WhileSubscribed(),
            initialValue = _viewState.value,
        )

    init {
        viewModelScope.launch {
            val lastSearchesDeferred = async { emptyStateProvider.getLastSearches() }
            val popularItemsDeferred = async { emptyStateProvider.getPopularItems() }

            _viewState.value = WooPosItemsSearchViewState.EmptySearchQuery(
                popularItems = popularItemsDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) },
                recentSearches = lastSearchesDeferred.await().let { it.take(minOf(MAX_ITEMS_COUNT, it.size)) },
            )
        }
    }

    private companion object {
        const val MAX_ITEMS_COUNT = 3
    }
}
