package com.woocommerce.android.ui.woopos.orders

import com.woocommerce.android.ui.woopos.orders.WooPosDetailPaneIssueRefundHandler.OrderSelectionAction
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class WooPosDetailPaneIssueRefundHandlerTest {
    private val handler = WooPosDetailPaneIssueRefundHandler()

    @Test
    fun `given no refund is open, when order selected, then order is selected`() {
        // WHEN
        val action = handler.handleOrderSelected(
            orderId = ORDER_ID,
            currentRefundOrderId = null,
            hasPendingChanges = false,
        )

        // THEN
        assertThat(action).isEqualTo(OrderSelectionAction.SelectOrder(ORDER_ID))
    }

    @Test
    fun `given same refund order is open, when order selected, then selection is ignored`() {
        // WHEN
        val action = handler.handleOrderSelected(
            orderId = ORDER_ID,
            currentRefundOrderId = ORDER_ID,
            hasPendingChanges = false,
        )

        // THEN
        assertThat(action).isEqualTo(OrderSelectionAction.Ignore)
    }

    @Test
    fun `given refund has pending changes, when different order selected, then confirmation is requested`() {
        // WHEN
        val action = handler.handleOrderSelected(
            orderId = NEXT_ORDER_ID,
            currentRefundOrderId = ORDER_ID,
            hasPendingChanges = true,
        )

        // THEN
        assertThat(action).isEqualTo(OrderSelectionAction.ConfirmPendingSelection(NEXT_ORDER_ID))
    }

    @Test
    fun `given refund has no pending changes, when different order selected, then dismiss is requested`() {
        // WHEN
        val action = handler.handleOrderSelected(
            orderId = NEXT_ORDER_ID,
            currentRefundOrderId = ORDER_ID,
            hasPendingChanges = false,
        )

        // THEN
        assertThat(action).isEqualTo(OrderSelectionAction.RequestRefundDismiss(NEXT_ORDER_ID))
    }

    @Test
    fun `given pending order selection, when refund dismissed, then refunded order is refreshed and pending order selected`() {
        // WHEN
        val action = handler.handleIssueRefundDismissed(
            refundedOrderId = ORDER_ID,
            pendingOrderSelectionAfterRefundDismiss = NEXT_ORDER_ID,
        )

        // THEN
        assertThat(action.refundedOrderId).isEqualTo(ORDER_ID)
        assertThat(action.orderIdToSelect).isEqualTo(NEXT_ORDER_ID)
    }

    private companion object {
        private const val ORDER_ID = 1L
        private const val NEXT_ORDER_ID = 2L
    }
}
