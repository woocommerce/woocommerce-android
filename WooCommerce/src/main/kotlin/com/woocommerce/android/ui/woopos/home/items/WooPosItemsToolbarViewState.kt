package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.cart.WooPosCartItemViewState
import com.woocommerce.android.ui.woopos.home.items.variations.WooPosVariationsNavigationData

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
        override val search: SearchState,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = SearchState.Hidden,
    )

    data class VariationList(
        override val tabs: List<Tab>,
        val variableProductData: WooPosVariationsNavigationData,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = SearchState.Hidden,
        backNavigation = true,
    )

    data class CustomAmountForm(
        override val tabs: List<Tab>,
        val editing: WooPosCartItemViewState.CustomAmount? = null,
    ) : WooPosItemsToolbarViewState(
        tabs = tabs,
        search = SearchState.Hidden,
        backNavigation = true,
    )

    sealed class Tab(open val name: String, open val highlightLevel: HighlightLevel) {
        enum class HighlightLevel {
            Full, Normal
        }

        data class Products(
            override val name: String,
            override val highlightLevel: HighlightLevel,
        ) : Tab(name, highlightLevel)

        data class Coupons(
            override val name: String,
            override val highlightLevel: HighlightLevel,
        ) : Tab(name, highlightLevel)

        data class Variations(
            override val name: String,
            override val highlightLevel: HighlightLevel,
        ) : Tab(name, highlightLevel)
    }

    sealed class SearchState {
        data class Visible(val state: WooPosSearchInputState) : SearchState()
        data object Hidden : SearchState()
    }
}
