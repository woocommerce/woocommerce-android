package com.woocommerce.android.ui.woopos.home.items

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import com.woocommerce.android.ui.woopos.home.items.WooPosItemsToolbarViewState.SearchState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosItemsToolbarViewStateTest {
    private val openSearch = SearchState.Visible(
        WooPosSearchInputState.Open(
            input = WooPosSearchInputState.Open.Input.Hint("Search coupons"),
            isLoading = false,
        )
    )

    @Test
    fun `given coupon list with open search, when isSearchOpen is read, then search is open`() {
        // GIVEN
        val state = WooPosItemsToolbarViewState.CouponList(tabs = emptyList(), search = openSearch)

        // WHEN
        val isSearchOpen = state.isSearchOpen

        // THEN
        assertThat(isSearchOpen).isTrue()
    }

    @Test
    fun `given coupon list with closed search, when isSearchOpen is read, then search is not open`() {
        // GIVEN
        val state = WooPosItemsToolbarViewState.CouponList(
            tabs = emptyList(),
            search = SearchState.Visible(WooPosSearchInputState.Closed),
        )

        // WHEN
        val isSearchOpen = state.isSearchOpen

        // THEN
        assertThat(isSearchOpen).isFalse()
    }

    @Test
    fun `given product list with open search, when isSearchOpen is read, then search is open`() {
        // GIVEN
        val state = WooPosItemsToolbarViewState.ProductList(tabs = emptyList(), search = openSearch)

        // WHEN
        val isSearchOpen = state.isSearchOpen

        // THEN
        assertThat(isSearchOpen).isTrue()
    }

    @Test
    fun `given product list with hidden search, when isSearchOpen is read, then search is not open`() {
        // GIVEN
        val state = WooPosItemsToolbarViewState.ProductList(tabs = emptyList(), search = SearchState.Hidden)

        // WHEN
        val isSearchOpen = state.isSearchOpen

        // THEN
        assertThat(isSearchOpen).isFalse()
    }
}
