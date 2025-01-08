package com.woocommerce.android.ui.payments.cardreader.payment.controller

import androidx.annotation.StringRes
import com.woocommerce.android.ui.payments.cardreader.payment.InteracRefundFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError

sealed class CardReaderPaymentOrRefundState {
    sealed class CardReaderPaymentState : CardReaderPaymentOrRefundState() {
        data class LoadingData(val onCancel: () -> Unit) : CardReaderPaymentState()

        data object ReFetchingOrder : CardReaderPaymentState()

        sealed class CollectingPayment(
            open val amountWithCurrencyLabel: String,
            @StringRes open val cardReaderHint: Int? = null,
        ) : CardReaderPaymentState() {
            data class BuiltInReaderCollectPaymentState(
                override val amountWithCurrencyLabel: String,
                override val cardReaderHint: Int? = null,
            ) : CollectingPayment(amountWithCurrencyLabel, cardReaderHint)

            data class ExternalReaderCollectPaymentState(
                override val amountWithCurrencyLabel: String,
                override val cardReaderHint: Int? = null,
                val onCancel: (() -> Unit)
            ) : CollectingPayment(amountWithCurrencyLabel, cardReaderHint)
        }

        sealed class ProcessingPayment(
            open val amountWithCurrencyLabel: String,
        ) : CardReaderPaymentState() {
            data class BuiltInReaderProcessingPayment(override val amountWithCurrencyLabel: String) :
                ProcessingPayment(amountWithCurrencyLabel)

            data class ExternalReaderProcessingPayment(
                override val amountWithCurrencyLabel: String,
                val onCancel: () -> Unit
            ) : ProcessingPayment(amountWithCurrencyLabel)
        }

        data class PrintingReceipt(val amountWithCurrencyLabel: String) : CardReaderPaymentState()

        sealed class PaymentCapturing(open val amountWithCurrencyLabel: String) :
            CardReaderPaymentState() {
            data class BuiltInReaderPaymentCapturing(override val amountWithCurrencyLabel: String) :
                PaymentCapturing(amountWithCurrencyLabel)

            data class ExternalReaderPaymentCapturing(override val amountWithCurrencyLabel: String) :
                PaymentCapturing(amountWithCurrencyLabel)
        }

        sealed class PaymentSuccessful(
            open val amountWithCurrencyLabel: String,
        ) : CardReaderPaymentState() {
            data class BuiltInReaderPaymentSuccessful(
                override val amountWithCurrencyLabel: String,
                val onPrintReceiptClicked: () -> Unit,
                val onSendReceiptClicked: () -> Unit,
                val onSaveUserClicked: () -> Unit
            ) : PaymentSuccessful(amountWithCurrencyLabel)

            data class ExternalReaderPaymentSuccessful(
                override val amountWithCurrencyLabel: String,
                val onPrintReceiptClicked: () -> Unit,
                val onSendReceiptClicked: () -> Unit,
                val onSaveUserClicked: () -> Unit
            ) : PaymentSuccessful(amountWithCurrencyLabel)

            data class BuiltInReaderPaymentSuccessfulReceiptSentAutomatically(
                override val amountWithCurrencyLabel: String,
                val recipientEmail: String,
                val onPrintReceiptClicked: () -> Unit,
                val onSaveUserClicked: () -> Unit
            ) : PaymentSuccessful(amountWithCurrencyLabel)

            data class ExternalReaderPaymentSuccessfulReceiptSentAutomatically(
                override val amountWithCurrencyLabel: String,
                val recipientEmail: String,
                val onPrintReceiptClicked: () -> Unit,
                val onSaveUserClicked: () -> Unit
            ) : PaymentSuccessful(amountWithCurrencyLabel)
        }

        sealed class PaymentFailed(
            open val errorType: PaymentFlowError,
            open val amountWithCurrencyLabel: String?,
            open val onCancel: (() -> Unit)? = null,
            open val onRetry: (() -> Unit)?,
            open val cta: CallToAction? = null,
        ) : CardReaderPaymentState() {
            sealed class BuiltInReaderFailedPayment(
                override val errorType: PaymentFlowError,
                override val amountWithCurrencyLabel: String?,
                override val onCancel: (() -> Unit)? = null,
                override val onRetry: (() -> Unit)? = null,
                override val cta: CallToAction? = null,
            ) : PaymentFailed(
                errorType,
                amountWithCurrencyLabel,
                onCancel,
                onRetry,
                cta,
            ) {
                data class Cancelable(
                    override val errorType: PaymentFlowError,
                    override val amountWithCurrencyLabel: String,
                    override val onCancel: (() -> Unit),
                    override val onRetry: (() -> Unit)? = null,
                    override val cta: CallToAction? = null,
                ) : BuiltInReaderFailedPayment(
                    errorType = errorType,
                    amountWithCurrencyLabel = amountWithCurrencyLabel,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    cta = cta,
                )
                data class NonCancelable(
                    override val errorType: PaymentFlowError,
                    override val onRetry: (() -> Unit),
                    override val cta: CallToAction? = null,
                ) : BuiltInReaderFailedPayment(
                    errorType,
                    amountWithCurrencyLabel = null,
                    onCancel = null,
                    onRetry = onRetry,
                    cta = cta,
                )
            }

            sealed class ExternalReaderFailedPayment(
                override val errorType: PaymentFlowError,
                override val amountWithCurrencyLabel: String?,
                override val onCancel: (() -> Unit)? = null,
                override val onRetry: (() -> Unit)? = null,
                override val cta: CallToAction? = null,
            ) : PaymentFailed(
                errorType,
                amountWithCurrencyLabel,
                onCancel,
                onRetry,
                cta,
            ) {
                data class Cancelable(
                    override val errorType: PaymentFlowError,
                    override val amountWithCurrencyLabel: String,
                    override val onCancel: (() -> Unit),
                    override val onRetry: (() -> Unit)? = null,
                    override val cta: CallToAction? = null,
                ) : ExternalReaderFailedPayment(
                    errorType = errorType,
                    amountWithCurrencyLabel = amountWithCurrencyLabel,
                    onCancel = onCancel,
                    onRetry = onRetry,
                    cta = cta,
                )
                data class NonCancelable(
                    override val errorType: PaymentFlowError,
                    override val onRetry: (() -> Unit),
                    override val cta: CallToAction? = null,
                ) : ExternalReaderFailedPayment(
                    errorType,
                    amountWithCurrencyLabel = null,
                    onCancel = null,
                    onRetry = onRetry,
                    cta = cta,
                )
            }
        }

        data object SharingReceipt : CardReaderPaymentState()
    }

    sealed class CardReaderInteracRefundState : CardReaderPaymentOrRefundState() {
        data class LoadingData(val onCancel: () -> Unit) : CardReaderInteracRefundState()

        data class CollectingInteracRefund(
            val amountWithCurrencyLabel: String,
            val onCancel: () -> Unit,
            @StringRes val cardReaderHint: Int? = null,
        ) : CardReaderInteracRefundState()

        data class ProcessingInteracRefund(
            val amountWithCurrencyLabel: String,
        ) : CardReaderInteracRefundState()

        sealed class InteracRefundFailure(
            open val amountWithCurrencyLabel: String?,
            open val errorType: InteracRefundFlowError,
            open val onRetry: (() -> Unit)? = null,
            open val onCancel: (() -> Unit)? = null,
            open val cta: CallToAction? = null,
        ) : CardReaderInteracRefundState() {
            data class Cancelable(
                override val amountWithCurrencyLabel: String,
                override val errorType: InteracRefundFlowError,
                override val onRetry: (() -> Unit)? = null,
                override val onCancel: (() -> Unit),
                override val cta: CallToAction? = null,
            ) : InteracRefundFailure(
                amountWithCurrencyLabel = amountWithCurrencyLabel,
                errorType = errorType,
                onRetry = onRetry,
                onCancel = onCancel,
                cta = cta,
            )
            data class NonCancelable(
                override val errorType: InteracRefundFlowError,
                override val onRetry: (() -> Unit),
                override val cta: CallToAction? = null,
            ) : InteracRefundFailure(
                amountWithCurrencyLabel = null,
                errorType = errorType,
                onRetry = onRetry,
                onCancel = null,
                cta = cta,
            )
        }

        data class InteracRefundSuccessful(
            val amountWithCurrencyLabel: String,
        ) : CardReaderInteracRefundState()
    }

    data class CallToAction(
        @StringRes val label: Int,
        val onCallToActionTapped: () -> Unit,
    )
}
