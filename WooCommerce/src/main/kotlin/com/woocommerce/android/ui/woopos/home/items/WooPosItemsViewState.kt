package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState

sealed class WooPosItemsViewState(
    open val tabs: List<Tab>,
    override val pullToRefreshState: WooPosPullToRefreshState,
) : WooPosBaseViewState(pullToRefreshState) {
    data class Content(
        val search: SearchState,
        override val items: List<WooPosItemSelectionViewState>,
        val bannerState: BannerState,
        override val paginationState: WooPosPaginationState = WooPosPaginationState.None,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
        override val tabs: List<Tab>,
    ) : WooPosItemsViewState(tabs, pullToRefreshState), WooPosContentViewState {
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
        override val tabs: List<Tab>,
        val withCart: Boolean
    ) : WooPosItemsViewState(tabs, pullToRefreshState)

    data class Error(
        override val tabs: List<Tab>,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosItemsViewState(tabs, pullToRefreshState)

    data class Empty(
        override val tabs: List<Tab>,
        override val pullToRefreshState: WooPosPullToRefreshState = WooPosPullToRefreshState.Enabled,
    ) : WooPosItemsViewState(tabs, pullToRefreshState)

    data class Tab(@StringRes val stringId: Int, val highlightLevel: HighlightLevel) {
        enum class HighlightLevel {
            Full, Normal
        }
    }
}
