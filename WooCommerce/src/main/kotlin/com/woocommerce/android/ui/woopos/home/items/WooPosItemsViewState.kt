package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState

sealed class WooPosItemsViewState(
    open val tabs: List<Tab>,
    open val search: SearchState,
) {
    data class ProductList(
        override val tabs: List<Tab>,
        override val search: SearchState,
    ) : WooPosItemsViewState(
        tabs = tabs,
        search = search,
    )

    data class CouponList(
        override val tabs: List<Tab>,
    ) : WooPosItemsViewState(
        tabs = tabs,
        search = SearchState.Hidden,
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
}
