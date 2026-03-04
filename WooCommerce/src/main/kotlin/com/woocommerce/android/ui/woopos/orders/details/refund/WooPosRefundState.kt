package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
sealed class WooPosRefundState {
    abstract val showCloseButton: Boolean

    @Immutable
    data object Loading : WooPosRefundState() {
        override val showCloseButton: Boolean = false
    }

    @Immutable
    data class Content(
        val orderId: Long,
        val orderNumber: String,
        val currency: String,
        val refundableItems: List<WooPosRefundableItem>,
        val selectedItemIds: Set<String>,
        val allItemsSelected: Boolean,
        val itemsCount: Int,
        val subtotal: BigDecimal,
        val taxes: BigDecimal,
        val total: BigDecimal,
        val formattedSubtotal: String,
        val formattedTaxes: String,
        val formattedTotal: String,
        val paymentMethod: String,
        val refundReason: String = "",
        val step: RefundStep
    ) : WooPosRefundState() {
        override val showCloseButton: Boolean
            get() = step != RefundStep.Processing

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
        val message: String,
        val errorType: ErrorType
    ) : WooPosRefundState() {
        override val showCloseButton: Boolean = true

        @Immutable
        enum class ErrorType {
            Loading,
            Processing
        }
    }

    @Immutable
    data object NoRefundableItems : WooPosRefundState() {
        override val showCloseButton: Boolean = true
    }

    @Immutable
    data class RefundSuccess(
        val orderId: Long,
        val orderNumber: String,
        val refundedAmount: String,
        val paymentMethod: String,
        val receiptSentMessage: String? = null,
    ) : WooPosRefundState() {
        override val showCloseButton: Boolean = true
    }
}
