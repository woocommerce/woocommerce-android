package com.woocommerce.android.ui.woopos.bookings

import com.woocommerce.android.R
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

class WooPosBookingsSearchHelper @Inject constructor(
    private val resourceProvider: ResourceProvider,
) {
    private lateinit var viewStateFlow: MutableStateFlow<WooPosBookingsState>

    fun initialize(viewStateFlow: MutableStateFlow<WooPosBookingsState>) {
        this.viewStateFlow = viewStateFlow
    }

    fun onSearchIconClicked() {
        updateSearchState(
            WooPosSearchInputState.Open(
                input = WooPosSearchInputState.Open.Input.Hint(
                    resourceProvider.getString(R.string.woopos_search_bookings)
                ),
                isLoading = false,
                requestFocus = true
            )
        )
    }

    fun onSearchChanged(query: String, cursorPosition: Int) {
        updateSearchState(
            WooPosSearchInputState.Open(
                input = WooPosSearchInputState.Open.Input.Query(query, cursorPosition),
                isLoading = false,
            )
        )
    }

    fun onClearSearchClicked() {
        updateSearchState(
            WooPosSearchInputState.Open(
                input = WooPosSearchInputState.Open.Input.Hint(
                    resourceProvider.getString(R.string.woopos_search_bookings)
                ),
                isLoading = false,
                requestFocus = true
            )
        )
    }

    fun onCloseSearchClicked() {
        updateSearchState(WooPosSearchInputState.Closed)
    }

    fun updateLoadingState(isLoading: Boolean) {
        val current = viewStateFlow.value
        val openState = current.searchInputState as? WooPosSearchInputState.Open ?: return
        updateSearchState(openState.copy(isLoading = isLoading))
    }

    fun getCurrentSearchQuery(): String? {
        return (
            (viewStateFlow.value.searchInputState as? WooPosSearchInputState.Open)
                ?.input as? WooPosSearchInputState.Open.Input.Query
            )?.query
    }

    fun isSearchOpen(): Boolean {
        return viewStateFlow.value.searchInputState is WooPosSearchInputState.Open
    }

    private fun updateSearchState(searchState: WooPosSearchInputState) {
        viewStateFlow.value = when (val current = viewStateFlow.value) {
            is WooPosBookingsState.Content -> current.copy(searchInputState = searchState)
            is WooPosBookingsState.Loading -> current.copy(searchInputState = searchState)
            is WooPosBookingsState.Error -> current
        }
    }
}
