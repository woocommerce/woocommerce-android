package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderInteracRefundState
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.CollectingPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.LoadingData
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentCapturing
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentSuccessful
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PrintingReceipt
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.ProcessingPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.ReFetchingOrder
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.SharingReceipt
import javax.inject.Inject

class CardReaderPaymentStateToViewStateMapper @Inject constructor(
    private val cardReaderPaymentReaderTypeStateProvider: CardReaderPaymentReaderTypeStateProvider,
) {
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    operator fun invoke(): (CardReaderPaymentOrRefundState) -> ViewState = { paymentState ->
        when (paymentState) {
            is CardReaderInteracRefundState.CollectingInteracRefund -> {
                ViewState.CollectRefundState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    onSecondaryActionClicked = paymentState.onCancel,
                    hintLabel = paymentState.cardReaderHint
                        ?: R.string.card_reader_interac_refund_refund_payment_hint,
                )
            }
            is CardReaderInteracRefundState.InteracRefundFailure -> {
                provideViewStateFor(paymentState)
            }
            is CardReaderInteracRefundState.InteracRefundSuccessful -> {
                ViewState.RefundSuccessfulState(paymentState.amountWithCurrencyLabel)
            }
            is CardReaderInteracRefundState.LoadingData -> {
                ViewState.RefundLoadingDataState(paymentState.onCancel)
            }
            is CardReaderInteracRefundState.ProcessingInteracRefund -> {
                ViewState.ProcessingRefundState(paymentState.amountWithCurrencyLabel)
            }
            is CollectingPayment.BuiltInReaderCollectPaymentState -> {
                ViewState.BuiltInReaderCollectPaymentState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    hintLabel = paymentState.cardReaderHint
                        ?: R.string.card_reader_payment_collect_payment_built_in_hint
                )
            }
            is CollectingPayment.ExternalReaderCollectPaymentState -> {
                ViewState.ExternalReaderCollectPaymentState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    hintLabel = paymentState.cardReaderHint
                        ?: R.string.card_reader_payment_collect_payment_hint,
                    onSecondaryActionClicked = paymentState.onCancel
                )
            }
            is LoadingData -> ViewState.LoadingDataState(paymentState.onCancel)
            is PaymentCapturing.BuiltInReaderPaymentCapturing -> {
                ViewState.BuiltInReaderCapturingPaymentState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                )
            }
            is PaymentCapturing.ExternalReaderPaymentCapturing -> {
                ViewState.ExternalReaderCapturingPaymentState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                )
            }
            is PaymentFailed.BuiltInReaderFailedPayment -> {
                provideViewStateFor(paymentState)
            }
            is PaymentFailed.ExternalReaderFailedPayment -> {
                provideViewStateFor(paymentState)
            }
            is PaymentSuccessful.BuiltInReaderPaymentSuccessful -> {
                ViewState.BuiltInReaderPaymentSuccessfulState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    onPrimaryActionClicked = paymentState.onPrintReceiptClicked,
                    onSecondaryActionClicked = paymentState.onSendReceiptClicked,
                    onTertiaryActionClicked = paymentState.onSaveUserClicked,
                )
            }
            is PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically -> {
                provideViewStateFor(paymentState)
            }
            is PaymentSuccessful.ExternalReaderPaymentSuccessful -> {
                ViewState.ExternalReaderPaymentSuccessfulState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    onPrimaryActionClicked = paymentState.onPrintReceiptClicked,
                    onSecondaryActionClicked = paymentState.onSendReceiptClicked,
                    onTertiaryActionClicked = paymentState.onSaveUserClicked,
                )
            }
            is PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically -> {
                provideViewStateFor(paymentState)
            }
            is PrintingReceipt -> ViewState.PrintingReceiptState(paymentState.amountWithCurrencyLabel)
            is ProcessingPayment.BuiltInReaderProcessingPayment -> {
                ViewState.BuiltInReaderProcessingPaymentState(paymentState.amountWithCurrencyLabel)
            }
            is ProcessingPayment.ExternalReaderProcessingPayment -> {
                ViewState.ExternalReaderProcessingPaymentState(
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    onSecondaryActionClicked = paymentState.onCancel
                )
            }
            ReFetchingOrder -> ViewState.ReFetchingOrderState
            SharingReceipt -> ViewState.SharingReceiptState
        }
    }

    private fun provideViewStateFor(
        paymentState: CardReaderInteracRefundState.InteracRefundFailure
    ): ViewState = when (paymentState) {
        is CardReaderInteracRefundState.InteracRefundFailure.Cancelable -> {
            provideViewStateFor(paymentState)
        }

        is CardReaderInteracRefundState.InteracRefundFailure.NonCancelable -> {
            ViewState.FailedRefundState(
                errorType = paymentState.errorType,
                primaryLabel = R.string.try_again,
                amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                onPrimaryActionClicked = paymentState.onRetry,
            )
        }
    }

    private fun provideViewStateFor(
        paymentState: CardReaderInteracRefundState.InteracRefundFailure.Cancelable
    ): ViewState =
        if (paymentState.onRetry == null) {
            if (paymentState.cta != null) {
                ViewState.FailedRefundState(
                    errorType = paymentState.errorType,
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = paymentState.cta.label,
                    onPrimaryActionClicked = paymentState.cta.onCallToActionTapped,
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = paymentState.onCancel,
                )
            } else {
                ViewState.FailedRefundState(
                    errorType = paymentState.errorType,
                    amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = R.string.card_reader_interac_refund_refund_failed_ok,
                    onPrimaryActionClicked = paymentState.onCancel,
                )
            }
        } else {
            ViewState.FailedRefundState(
                errorType = paymentState.errorType,
                amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
                primaryLabel = R.string.try_again,
                onPrimaryActionClicked = paymentState.onRetry,
                secondaryLabel = R.string.cancel,
                onSecondaryActionClicked = paymentState.onCancel,
            )
        }

    private fun provideViewStateFor(
        paymentState: PaymentFailed.ExternalReaderFailedPayment
    ): ViewState = when (paymentState) {
        is PaymentFailed.ExternalReaderFailedPayment.Cancelable -> {
            provideViewStateFor(paymentState)
        }

        is PaymentFailed.ExternalReaderFailedPayment.NonCancelable -> {
            cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                cardReaderType = CardReaderType.EXTERNAL,
                errorType = paymentState.errorType,
                amountLabel = paymentState.amountWithCurrencyLabel,
                primaryLabel = R.string.try_again,
                onPrimaryActionClicked = paymentState.onRetry
            )
        }
    }

    private fun provideViewStateFor(
        paymentState: PaymentFailed.ExternalReaderFailedPayment.Cancelable
    ): ViewState =
        if (paymentState.cta != null) {
            cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                cardReaderType = CardReaderType.EXTERNAL,
                errorType = paymentState.errorType,
                amountLabel = paymentState.amountWithCurrencyLabel,
                primaryLabel = paymentState.cta.label,
                onPrimaryActionClicked = paymentState.cta.onCallToActionTapped,
                secondaryLabel = R.string.cancel,
                onSecondaryActionClicked = paymentState.onCancel,
            )
        } else {
            if (paymentState.onRetry != null) {
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = CardReaderType.EXTERNAL,
                    errorType = paymentState.errorType,
                    amountLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = R.string.try_again,
                    onPrimaryActionClicked = paymentState.onRetry,
                    secondaryLabel = R.string.cancel,
                    onSecondaryActionClicked = paymentState.onCancel,
                )
            } else {
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = CardReaderType.EXTERNAL,
                    errorType = paymentState.errorType,
                    amountLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = R.string.card_reader_payment_payment_failed_ok,
                    onPrimaryActionClicked = paymentState.onCancel!!,
                )
            }
        }

    private fun provideViewStateFor(
        paymentState: PaymentFailed.BuiltInReaderFailedPayment
    ): ViewState = when (paymentState) {
        is PaymentFailed.BuiltInReaderFailedPayment.Cancelable -> {
            provideViewStateFor(paymentState)
        }

        is PaymentFailed.BuiltInReaderFailedPayment.NonCancelable -> {
            cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                cardReaderType = CardReaderType.BUILT_IN,
                errorType = paymentState.errorType,
                amountLabel = paymentState.amountWithCurrencyLabel,
                primaryLabel = R.string.try_again,
                onPrimaryActionClicked = paymentState.onRetry,
            )
        }
    }

    private fun provideViewStateFor(
        paymentState: PaymentFailed.BuiltInReaderFailedPayment.Cancelable
    ): ViewState =
        if (paymentState.cta != null) {
            cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                cardReaderType = CardReaderType.BUILT_IN,
                errorType = paymentState.errorType,
                amountLabel = paymentState.amountWithCurrencyLabel,
                primaryLabel = paymentState.cta.label,
                onPrimaryActionClicked = paymentState.cta.onCallToActionTapped,
                secondaryLabel = R.string.cancel,
                onSecondaryActionClicked = paymentState.onCancel,
            )
        } else {
            if (paymentState.onRetry != null) {
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = CardReaderType.BUILT_IN,
                    errorType = paymentState.errorType,
                    amountLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = R.string.try_again,
                    onPrimaryActionClicked = paymentState.onRetry,
                    onSecondaryActionClicked = paymentState.onCancel,
                    secondaryLabel = R.string.cancel,
                )
            } else {
                cardReaderPaymentReaderTypeStateProvider.provideFailedPaymentState(
                    cardReaderType = CardReaderType.BUILT_IN,
                    errorType = paymentState.errorType,
                    amountLabel = paymentState.amountWithCurrencyLabel,
                    primaryLabel = R.string.card_reader_payment_payment_failed_ok,
                    onPrimaryActionClicked = paymentState.onCancel,
                )
            }
        }

    private fun provideViewStateFor(
        paymentState: PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically
    ): ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState {
        val receiptSentHint = UiString.UiStringRes(
            R.string.card_reader_payment_reader_receipt_sent,
            listOf(UiString.UiStringText(paymentState.recipientEmail)),
            true
        )
        return ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState(
            amountWithCurrencyLabel = paymentState.amountWithCurrencyLabel,
            receiptSentAutomaticallyHint = receiptSentHint,
            onPrimaryActionClicked = paymentState.onPrintReceiptClicked,
            onTertiaryActionClicked = paymentState.onSaveUserClicked,
        )
    }

    private fun provideViewStateFor(
        paymentState: PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically
    ): ViewState {
        val receiptSentHint = UiString.UiStringRes(
            R.string.card_reader_payment_reader_receipt_sent,
            listOf(UiString.UiStringText(paymentState.recipientEmail)),
            true
        )
        return cardReaderPaymentReaderTypeStateProvider.providePaymentSuccessfulReceiptSentAutomaticallyState(
            cardReaderType = CardReaderType.EXTERNAL,
            amountLabel = paymentState.amountWithCurrencyLabel,
            receiptSentHint = receiptSentHint,
            onSaveUserClicked = paymentState.onSaveUserClicked,
            onPrintReceiptClicked = paymentState.onPrintReceiptClicked,
        )
    }
}
