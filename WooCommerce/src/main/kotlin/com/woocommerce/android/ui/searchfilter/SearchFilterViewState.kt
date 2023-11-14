package com.woocommerce.android.ui.searchfilter

sealed class SearchFilterViewState {
    data class Loaded(
        val searchFilterItems: List<SearchFilterItem>,
        val searchHint: String
    ) : SearchFilterViewState()

    data class Search(
        val searchFilterItems: List<SearchFilterItem>
    ) : SearchFilterViewState()

    data class Empty(
        val searchQuery: String
    ) : SearchFilterViewState()
}
