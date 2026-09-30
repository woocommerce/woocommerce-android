package com.woocommerce.android.ui.orders.list

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class OrderListScreenStateTest {
    @Test
    fun `given browsing mode, when checking FAB visibility, then it is visible`() {
        // GIVEN
        val state = OrderListScreenState()

        // WHEN
        val isVisible = state.shouldShowCreateOrderFab

        // THEN
        assertThat(isVisible).isTrue()
    }

    @Test
    fun `given search mode, when checking FAB visibility, then it is hidden`() {
        // GIVEN
        val state = OrderListScreenState(isSearchActive = true)

        // WHEN
        val isVisible = state.shouldShowCreateOrderFab

        // THEN
        assertThat(isVisible).isFalse()
    }

    @Test
    fun `given selection mode, when checking FAB visibility, then it is hidden`() {
        // GIVEN
        val state = OrderListScreenState(
            rowState = OrderListRowState(bulkSelectedOrderIds = setOf(ORDER_ID)),
        )

        // WHEN
        val isVisible = state.shouldShowCreateOrderFab

        // THEN
        assertThat(isVisible).isFalse()
    }

    @Test
    fun `given FAB is unavailable, when browsing, then it is hidden`() {
        // GIVEN
        val state = OrderListScreenState(showCreateOrderFab = false)

        // WHEN
        val isVisible = state.shouldShowCreateOrderFab

        // THEN
        assertThat(isVisible).isFalse()
    }

    private companion object {
        const val ORDER_ID = 1L
    }
}
