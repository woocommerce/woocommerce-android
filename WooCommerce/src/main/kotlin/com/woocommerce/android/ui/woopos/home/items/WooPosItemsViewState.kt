package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState

sealed class WooPosItemsViewState private constructor(
    open val tabs: List<Tab>,
    open val search: SearchState,
    open val banner: BannerState,
) {
    data class ProductList(
        override val tabs: List<Tab>,
        override val search: SearchState,
        override val banner: BannerState,
    ) : WooPosItemsViewState(
        tabs = tabs,
        search = search,
        banner = banner,
    )

    data class CouponList(
        override val tabs: List<Tab>,
    ) : WooPosItemsViewState(
        tabs = tabs,
        search = SearchState.Hidden,
        banner = BannerState.Hidden,
    )

    data class Tab(@StringRes val stringId: Int, val highlightLevel: HighlightLevel) {
        enum class HighlightLevel {
            Full, Normal
        }
    }

    sealed class SearchState {
        data class Visible(val state: WooPosSearchInputState) : SearchState()
        data object Hidden : SearchState()
    }

    sealed class BannerState() {
        data class Visible(
            @StringRes val title: Int,
            @StringRes val message: Int,
            @DrawableRes val icon: Int
        ) : BannerState()

        data object Hidden : BannerState()
    }
}
