package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
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
        val step: RefundStep,
        // Server-calculated preview status. When server refunds are unavailable the store falls
        // back to the local calculation and these stay at their defaults (no loading/no failure).
        val isPreviewLoading: Boolean = false,
        val previewFailed: Boolean = false,
    ) : WooPosRefundState() {

        @Immutable
        sealed class RefundStep {
            fun isNonCancelable(): Boolean {
                return when (val step = this) {
                    Processing,
                    ProcessingRefund,
                    NotifyingStore -> true
                    is ReadyForRefund -> step.isDismissBlocked
                    SelectItems,
                    ReviewRefund,
                    ConfirmRefund,
                    PreparingReader,
                    ReaderDisconnected -> false
                }
            }

            @Immutable
            data object SelectItems : RefundStep()

            @Immutable
            data object ReviewRefund : RefundStep()

            @Immutable
            data object ConfirmRefund : RefundStep()

            @Immutable
            data object Processing : RefundStep()

            @Immutable
            data object PreparingReader : RefundStep()

            @Immutable
            data object ReaderDisconnected : RefundStep()

            @Immutable
            data class ReadyForRefund(
                @StringRes val cardReaderHint: Int? = null,
                val isDismissBlocked: Boolean = false,
            ) : RefundStep()

            @Immutable
            data object ProcessingRefund : RefundStep()

            @Immutable
            data object NotifyingStore : RefundStep()
        }
    }

    @Immutable
    data class Error(
        val message: String,
        val errorType: ErrorType,
        val canRetry: Boolean = true,
    ) : WooPosRefundState() {

        @Immutable
        enum class ErrorType {
            Loading,
            Processing
        }
    }

    @Immutable
    data object NoRefundableItems : WooPosRefundState()

    @Immutable
    data class RefundSuccess(
        val orderId: Long,
        val orderNumber: String,
        val refundedAmount: String,
        val paymentMethod: String,
        val receiptSentMessage: String? = null,
    ) : WooPosRefundState()
}
