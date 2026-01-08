package com.woocommerce.android.ui.woopos.orders

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
sealed class WooPosRefundState {
    @Immutable
    data object Loading : WooPosRefundState()

    @Immutable
    data class Content(
        val orderId: Long,
        val orderNumber: String,
        val currency: String,
        val refundableItems: List<WooPosRefundableItem>,
        val itemsCount: Int,
        val subtotal: BigDecimal,
        val taxes: BigDecimal,
        val total: BigDecimal,
        val formattedSubtotal: String,
        val formattedTaxes: String,
        val formattedTotal: String,
        val paymentMethod: String,
        val refundReason: String = "",
        val isEditingReason: Boolean = false,
        val step: RefundStep
    ) : WooPosRefundState() {
        @Immutable
        sealed class RefundStep {
            @Immutable
            data object SelectItems : RefundStep()

            @Immutable
            data object ReviewRefund : RefundStep()

            @Immutable
            data object ConfirmRefund : RefundStep()

            @Immutable
            data object Processing : RefundStep()
        }
    }

    @Immutable
    data class Error(
        val message: String
    ) : WooPosRefundState()

    @Immutable
    data object NoRefundableItems : WooPosRefundState()

    @Immutable
    data class RefundSuccess(
        val orderId: Long,
        val orderNumber: String,
        val refundedAmount: String
    ) : WooPosRefundState()

    @Immutable
    data class RefundError(
        val message: String
    ) : WooPosRefundState()
}
