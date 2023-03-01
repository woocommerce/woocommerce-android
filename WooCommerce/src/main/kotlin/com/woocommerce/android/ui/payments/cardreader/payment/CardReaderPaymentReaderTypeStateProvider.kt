package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.BUILT_IN
import com.woocommerce.android.ui.payments.cardreader.onboarding.CardReaderType.EXTERNAL
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.BuiltInReaderProcessingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCapturingPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderCollectPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderFailedPaymentState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderPaymentSuccessfulState
import com.woocommerce.android.ui.payments.cardreader.payment.ViewState.ExternalReaderProcessingPaymentState
import javax.inject.Inject

class CardReaderPaymentReaderTypeStateProvider @Inject constructor() {
    fun provideCollectPaymentState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        onCancelPaymentFlow: (() -> Unit),
    ): ViewState = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderCollectPaymentState(amountLabel)
        EXTERNAL -> ExternalReaderCollectPaymentState(
            amountLabel,
            onSecondaryActionClicked = onCancelPaymentFlow
        )
    }

    fun provideProcessingPaymentState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        onCancelPaymentFlow: (() -> Unit),
    ): ViewState = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderProcessingPaymentState(amountLabel)
        EXTERNAL -> ExternalReaderProcessingPaymentState(
            amountLabel,
            onSecondaryActionClicked = onCancelPaymentFlow
        )
    }

    fun providePaymentSuccessState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        onPrintReceiptClicked: () -> Unit,
        onSendReceiptClicked: () -> Unit,
        onSaveUserClicked: () -> Unit
    ) = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderPaymentSuccessfulState(
            amountLabel,
            onPrintReceiptClicked,
            onSendReceiptClicked,
            onSaveUserClicked
        )
        EXTERNAL -> ExternalReaderPaymentSuccessfulState(
            amountLabel,
            onPrintReceiptClicked,
            onSendReceiptClicked,
            onSaveUserClicked
        )
    }

    fun providePaymentSuccessfulReceiptSentAutomaticallyState(
        cardReaderType: CardReaderType,
        amountLabel: String,
        receiptSentHint: UiStringRes,
        onPrintReceiptClicked: () -> Unit,
        onSaveUserClicked: () -> Unit
    ) = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState(
            amountLabel,
            receiptSentHint,
            onPrintReceiptClicked,
            onSaveUserClicked
        )
        EXTERNAL -> ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState(
            amountLabel,
            receiptSentHint,
            onPrintReceiptClicked,
            onSaveUserClicked
        )
    }

    fun provideFailedPaymentState(
        cardReaderType: CardReaderType,
        errorType: PaymentFlowError,
        amountLabel: String?,
        primaryLabel: Int? = R.string.try_again,
        onPrimaryActionClicked: () -> Unit,
        secondaryLabel: Int? = null,
        onSecondaryActionClicked: (() -> Unit)? = null,
    ) = when (cardReaderType) {
        BUILT_IN -> BuiltInReaderFailedPaymentState(
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            primaryLabel = primaryLabel,
            onPrimaryActionClicked = onPrimaryActionClicked,
            secondaryLabel = secondaryLabel,
            onSecondaryActionClicked = onSecondaryActionClicked
        )
        EXTERNAL -> ExternalReaderFailedPaymentState(
            errorType = errorType,
            amountWithCurrencyLabel = amountLabel,
            primaryLabel = primaryLabel,
            onPrimaryActionClicked = onPrimaryActionClicked,
            secondaryLabel = secondaryLabel,
            onSecondaryActionClicked = onSecondaryActionClicked
        )
    }

    fun provideCapturingPaymentState(cardReaderType: CardReaderType, amountLabel: String): ViewState =
        when (cardReaderType) {
            BUILT_IN -> BuiltInReaderCapturingPaymentState(amountLabel)
            EXTERNAL -> ExternalReaderCapturingPaymentState(amountLabel)
        }
}
