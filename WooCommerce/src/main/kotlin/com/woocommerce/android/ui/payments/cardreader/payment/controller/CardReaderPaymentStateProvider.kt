package com.woocommerce.android.ui.payments.cardreader.payment.controller

import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.EXTERNAL
import com.woocommerce.android.ui.payments.cardreader.payment.PaymentFlowError
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.CollectingPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentCapturing
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed.BuiltInReaderFailedPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentFailed.ExternalReaderFailedPayment
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.PaymentSuccessful
import com.woocommerce.android.ui.payments.cardreader.payment.controller.CardReaderPaymentOrRefundState.CardReaderPaymentState.ProcessingPayment
import javax.inject.Inject

class CardReaderPaymentStateProvider @Inject constructor() {
    fun provideCancellableFailedPaymentState(
        cardReaderType: CardReaderType,
        errorType: PaymentFlowError,
        amountWithCurrencyLabel: String,
        onCancel: (() -> Unit),
        onRetry: (() -> Unit)? = null,
        cta: CardReaderPaymentOrRefundState.CallToAction? = null,
    ) = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderFailedPayment.Cancelable(
            errorType = errorType,
            amountWithCurrencyLabel = amountWithCurrencyLabel,
            onCancel = onCancel,
            onRetry = onRetry,
            cta = cta
        )
        EXTERNAL -> ExternalReaderFailedPayment.Cancelable(
            errorType = errorType,
            amountWithCurrencyLabel = amountWithCurrencyLabel,
            onCancel = onCancel,
            onRetry = onRetry,
            cta = cta
        )
    }

    fun provideNonCancellableFailedPaymentState(
        cardReaderType: CardReaderType,
        errorType: PaymentFlowError,
        onRetry: (() -> Unit),
        cta: CardReaderPaymentOrRefundState.CallToAction? = null,
    ) = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderFailedPayment.NonCancelable(
            errorType = errorType,
            onRetry = onRetry,
            cta = cta
        )
        EXTERNAL -> ExternalReaderFailedPayment.NonCancelable(
            errorType = errorType,
            onRetry = onRetry,
            cta = cta
        )
    }

    fun provideCollectingPaymentState(
        cardReaderType: CardReaderType,
        amountWithCurrencyLabel: String,
        onCancel: () -> Unit
    ) = when (cardReaderType) {
        BUILT_IN -> CollectingPayment.BuiltInReaderCollectPaymentState(
            amountWithCurrencyLabel = amountWithCurrencyLabel
        )

        EXTERNAL -> CollectingPayment.ExternalReaderCollectPaymentState(
            amountWithCurrencyLabel = amountWithCurrencyLabel,
            onCancel = onCancel,
        )
    }

    fun provideProcessingPaymentState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        onCancel: () -> Unit
    ): ProcessingPayment = when (cardReaderType) {
        BUILT_IN -> ProcessingPayment.BuiltInReaderProcessingPayment(
            amountWithCurrencyLabel = amountLabel,
        )
        EXTERNAL -> ProcessingPayment.ExternalReaderProcessingPayment(
            amountWithCurrencyLabel = amountLabel,
            onCancel = onCancel,
        )
    }

    fun provideCapturingPaymentState(
        cardReaderType: CardReaderType,
        amountLabel: String
    ): PaymentCapturing = when (cardReaderType) {
        BUILT_IN -> PaymentCapturing.BuiltInReaderPaymentCapturing(amountLabel)
        EXTERNAL -> PaymentCapturing.ExternalReaderPaymentCapturing(amountLabel)
    }

    fun providePaymentSuccessfulReceiptSentAutomaticallyState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        recipientEmail: String,
        onPrintReceiptClicked: () -> Unit,
        onSaveUserClicked: () -> Unit
    ): CardReaderPaymentOrRefundState = when (cardReaderType) {
        BUILT_IN -> PaymentSuccessful.BuiltInReaderPaymentSuccessfulReceiptSentAutomatically(
            amountWithCurrencyLabel = amountLabel,
            recipientEmail = recipientEmail,
            onPrintReceiptClicked = onPrintReceiptClicked,
            onSaveUserClicked = onSaveUserClicked
        )
        EXTERNAL -> PaymentSuccessful.ExternalReaderPaymentSuccessfulReceiptSentAutomatically(
            amountWithCurrencyLabel = amountLabel,
            recipientEmail = recipientEmail,
            onPrintReceiptClicked = onPrintReceiptClicked,
            onSaveUserClicked = onSaveUserClicked
        )
    }

    fun providePaymentSuccessState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        onPrintReceiptClicked: () -> Unit,
        onSendReceiptClicked: () -> Unit,
        onSaveUserClicked: () -> Unit
    ): PaymentSuccessful = when (cardReaderType) {
        BUILT_IN -> PaymentSuccessful.BuiltInReaderPaymentSuccessful(
            amountWithCurrencyLabel = amountLabel,
            onSendReceiptClicked = onSendReceiptClicked,
            onPrintReceiptClicked = onPrintReceiptClicked,
            onSaveUserClicked = onSaveUserClicked
        )
        EXTERNAL -> PaymentSuccessful.ExternalReaderPaymentSuccessful(
            amountWithCurrencyLabel = amountLabel,
            onSendReceiptClicked = onSendReceiptClicked,
            onPrintReceiptClicked = onPrintReceiptClicked,
            onSaveUserClicked = onSaveUserClicked
        )
    }
}
