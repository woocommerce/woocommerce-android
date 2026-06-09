package com.woocommerce.android.ui.woopos.orders

internal class WooPosDetailPaneIssueRefundHandler {
    fun handleOrderSelected(
        orderId: Long,
        currentRefundOrderId: Long?,
        hasPendingChanges: Boolean,
    ): OrderSelectionAction {
        return when {
            currentRefundOrderId == null -> OrderSelectionAction.SelectOrder(orderId)
            currentRefundOrderId == orderId -> OrderSelectionAction.Ignore
            hasPendingChanges -> OrderSelectionAction.ConfirmPendingSelection(orderId)
            else -> OrderSelectionAction.RequestRefundDismiss(orderId)
        }
    }

    fun handleIssueRefundDismissed(
        refundedOrderId: Long?,
        pendingOrderSelectionAfterRefundDismiss: Long?,
    ): IssueRefundDismissedAction {
        return IssueRefundDismissedAction(
            refundedOrderId = refundedOrderId,
            orderIdToSelect = pendingOrderSelectionAfterRefundDismiss,
        )
    }

    sealed interface OrderSelectionAction {
        data class SelectOrder(val orderId: Long) : OrderSelectionAction
        data object Ignore : OrderSelectionAction
        data class ConfirmPendingSelection(val orderId: Long) : OrderSelectionAction
        data class RequestRefundDismiss(val orderId: Long) : OrderSelectionAction
    }

    data class IssueRefundDismissedAction(
        val refundedOrderId: Long?,
        val orderIdToSelect: Long?,
    )
}
