package com.woocommerce.android.ui.woopos.home.items

import androidx.annotation.StringRes
import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariableProductNavigationData

sealed class WooPosItemsToolbarViewState(
    open val tabs: List<Tab>,
    open val search: SearchState,
    open val backNavigation: Boolean = false,
) {
    data class ProductList(
        override val tabs: List<Tab>,
        override val search: SearchState,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = search,
    )

    data class CouponList(
        override val tabs: List<Tab>,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = SearchState.Hidden,
    )

    data class VariationList(
        override val tabs: List<Tab>,
        val variableProductData: WooPosVariableProductNavigationData,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = SearchState.Hidden,
        backNavigation = true,
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
