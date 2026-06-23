package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.ui.woopos.common.composeui.component.WooPosSearchInputState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosOrdersScreenTest {

    @Test
    fun `given search closed and not issuing refund, when resolving toolbar visibility, then toolbar is shown`() {
        // WHEN
        val result = shouldShowOrdersToolbar(
            searchInputState = WooPosSearchInputState.Closed,
            isIssuingRefund = false,
        )

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given a refund is being issued, when resolving toolbar visibility, then toolbar is hidden`() {
        // WHEN — the refund flow is full screen with its own header, so the orders toolbar must hide.
        val result = shouldShowOrdersToolbar(
            searchInputState = WooPosSearchInputState.Closed,
            isIssuingRefund = true,
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given search is open, when resolving toolbar visibility, then toolbar is hidden`() {
        // WHEN
        val result = shouldShowOrdersToolbar(
            searchInputState = openSearch(),
            isIssuingRefund = false,
        )

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given search open and issuing refund, when resolving toolbar visibility, then toolbar is hidden`() {
        // WHEN
        val result = shouldShowOrdersToolbar(
            searchInputState = openSearch(),
            isIssuingRefund = true,
        )

        // THEN
        assertThat(result).isFalse()
    }

    private fun openSearch() = WooPosSearchInputState.Open(
        input = WooPosSearchInputState.Open.Input.Hint("Search"),
        isLoading = false,
    )
}
