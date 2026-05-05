package com.woocommerce.android.ui.woopos.orders.details

import androidx.compose.runtime.Immutable
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemRow

@Immutable
sealed class WooPosOrderDetailsState {
    @Immutable
    data object Idle : WooPosOrderDetailsState()

    @Immutable
    data object Loading : WooPosOrderDetailsState()

    @Immutable
    data class Loaded(
        val details: Details,
        val dialogState: DialogState
    ) : WooPosOrderDetailsState()

    @Immutable
    data class Error(val message: String) : WooPosOrderDetailsState()

    @Immutable
    sealed class DialogState {
        data object Hidden : DialogState()

        data class RefundDetails(
            val label: String,
            val items: List<LineItemRow>,
            val itemsSubtotalLabel: String,
            val itemsSubtotalAmount: String,
            val tax: String,
            val refundTotal: String,
            val paymentMethodTitle: String?,
        ) : DialogState()
    }
}
