package com.woocommerce.android.ui.woopos.home.items.providers

import com.woocommerce.android.ui.woopos.home.items.WooPosItemSelectionViewState
import com.woocommerce.android.ui.woopos.home.items.WooPosPaginationState
import com.woocommerce.android.ui.woopos.home.items.WooPosPullToRefreshState
import kotlinx.coroutines.flow.Flow

interface WooPosItemDataProvider {
    val data: Flow<DataProviderState>
    suspend fun init()
    suspend fun fetchItems(forceRefresh: Boolean)
    suspend fun loadMore()
}

data class DataProviderState(
    val items: List<WooPosItemSelectionViewState>,
    val error: String?,
    val pullToRefreshState: WooPosPullToRefreshState,
    val paginationState: WooPosPaginationState,
) {
    companion object {
        fun fetching(
            data: List<WooPosItemSelectionViewState> = emptyList(),
            isPullToRefresh: Boolean
        ): DataProviderState =
            DataProviderState(
                items = data,
                error = null,
                pullToRefreshState = if (isPullToRefresh)
                    WooPosPullToRefreshState.Refreshing
                else
                    WooPosPullToRefreshState.Disabled,
                paginationState = WooPosPaginationState.None
            )

        fun dataShown(data: List<WooPosItemSelectionViewState>): DataProviderState =
            DataProviderState(
                items = data,
                error = null,
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                paginationState = WooPosPaginationState.None
            )

        fun remoteRequestFailed(data: List<WooPosItemSelectionViewState>, remoteError: String): DataProviderState =
            DataProviderState(
                items = data,
                error = remoteError,
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                paginationState = WooPosPaginationState.None
            )

        fun loadingMore(data: List<WooPosItemSelectionViewState>): DataProviderState =
            DataProviderState(
                items = data,
                error = null,
                pullToRefreshState = WooPosPullToRefreshState.Disabled,
                paginationState = WooPosPaginationState.Loading
            )

        fun loadingMoreFailed(data: List<WooPosItemSelectionViewState>, loadMoreError: String): DataProviderState =
            DataProviderState(
                items = data,
                error = loadMoreError,
                pullToRefreshState = WooPosPullToRefreshState.Enabled,
                paginationState = WooPosPaginationState.Error
            )
    }
}
