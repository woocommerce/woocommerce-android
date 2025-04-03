package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState

sealed class WooPosItemsViewState(
    override val reloadingWithPullToRefresh: Boolean,
) : WooPosBaseViewState(reloadingWithPullToRefresh) {
    data class Content(
        val search: SearchState,
        override val items: List<WooPosItemSelectionViewState>,
        val bannerState: BannerState,
        override val paginationState: PaginationState = PaginationState.None,
        override val reloadingWithPullToRefresh: Boolean = false,
        val couponsEnabled: Boolean = false,
    ) : WooPosItemsViewState(reloadingWithPullToRefresh), ContentViewState {
        data class BannerState(
            val isBannerHiddenByUser: Boolean,
            @StringRes val title: Int,
            @StringRes val message: Int,
            @DrawableRes val icon: Int
        )

        sealed class SearchState {
            data class Visible(val state: WooPosSearchInputState) : SearchState()
            object Hidden : SearchState()
        }
    }

    data class Loading(
        override val reloadingWithPullToRefresh: Boolean = false,
        val withCart: Boolean
    ) : WooPosItemsViewState(reloadingWithPullToRefresh)

    data class Error(
        override val reloadingWithPullToRefresh: Boolean = false
    ) : WooPosItemsViewState(reloadingWithPullToRefresh)

    data class Empty(
        override val reloadingWithPullToRefresh: Boolean = false
    ) : WooPosItemsViewState(reloadingWithPullToRefresh)
}
