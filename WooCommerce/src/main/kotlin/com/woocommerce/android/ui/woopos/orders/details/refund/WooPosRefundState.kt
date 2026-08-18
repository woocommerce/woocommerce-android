package com.woocommerce.android.ui.woopos.orders.details.refund

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import java.math.BigDecimal

@Immutable
sealed class WooPosRefundState {

    /**
     * What the cashier can do about a failure. Mapped refund errors always fail the same way, so
     * [RefreshItems] reloads the order and its refunds and [None] only leaves the flow. [Retry] is
     * for failures that can pass on a second try, such as a network error.
     */
    enum class Recovery {
        Retry,
        RefreshItems,
        None,
    }

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
        val isPreviewLoading: Boolean = false,
        val previewFailure: PreviewFailure? = null,
    ) : WooPosRefundState() {

        /**
         * A failed preview, shown on the selection step. [message] is the store's mapped copy, or
         * null to show the generic one.
         */
        @Immutable
        data class PreviewFailure(
            val message: String?,
            val recovery: Recovery,
        )

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
        val recovery: Recovery = Recovery.Retry,
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
