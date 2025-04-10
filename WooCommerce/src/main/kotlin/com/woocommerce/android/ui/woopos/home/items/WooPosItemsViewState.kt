package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState

sealed class WooPosItemsViewState(
    override val pullToRefreshState: WooPosPullToRefreshState,
) : WooPosBaseViewState(pullToRefreshState) {
    data class Content(
        val search: SearchState,
        override val items: List<WooPosItemSelectionViewState>,
        val bannerState: BannerState,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        val couponsEnabled: Boolean = false,
    ) : WooPosItemsViewState(pullToRefreshState), WooPosContentViewState {
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
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        val withCart: Boolean
    ) : WooPosItemsViewState(pullToRefreshState)

    data class Error(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosItemsViewState(pullToRefreshState)

    data class Empty(
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosItemsViewState(pullToRefreshState)
}
