package com.woocommerce.android.ui.payments.cardreader.payment

import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes

interface TrackableState {
    val nameForTracking: String
}

interface PaymentFlow : TrackableState

interface InteracRefundFlow : TrackableState

sealed class ViewState(
    @StringRes open val hintLabel: Int? = null,
    @StringRes open val headerLabel: Int? = null,
    val paymentStateLabel: UiString? = null,
    open val receiptSentAutomaticallyHint: UiString? = null,
    @DimenRes val paymentStateLabelTopMargin: Int = R.dimen.major_275,
    @DrawableRes val illustration: Int? = null,
    open val isProgressVisible: Boolean = false,
    val primaryActionLabel: Int? = null,
    val secondaryActionLabel: Int? = null,
    val tertiaryActionLabel: Int? = null,
) {
    open val onPrimaryActionClicked: (() -> Unit)? = null
    open val onSecondaryActionClicked: (() -> Unit)? = null
    open val onTertiaryActionClicked: (() -> Unit)? = null
    open val amountWithCurrencyLabel: String? = null

    data class LoadingDataState(
        override val onSecondaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_collect_payment_loading_header,
        hintLabel = R.string.card_reader_payment_collect_payment_loading_hint,
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_collect_payment_loading_payment_state),
        isProgressVisible = true,
        secondaryActionLabel = R.string.cancel
    ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Loading"
    }

    data class ExternalReaderFailedPaymentState(
        private val errorType: PaymentFlowError,
        override val amountWithCurrencyLabel: String?,
        private val primaryLabel: Int? = R.string.try_again,
        private val secondaryLabel: Int? = null,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit)? = null,
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_payment_failed_header,
        paymentStateLabel = errorType.message,
        paymentStateLabelTopMargin = R.dimen.major_100,
        primaryActionLabel = primaryLabel,
        illustration = R.drawable.img_products_error,
        secondaryActionLabel = secondaryLabel
    )

    data class BuiltInReaderFailedPaymentState(
        private val errorType: PaymentFlowError,
        override val amountWithCurrencyLabel: String?,
        private val primaryLabel: Int? = R.string.try_again,
        private val secondaryLabel: Int? = null,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit)? = null,
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_payment_failed_header,
        paymentStateLabel = errorType.message,
        paymentStateLabelTopMargin = R.dimen.major_100,
        primaryActionLabel = primaryLabel,
        illustration = R.drawable.img_card_reader_tpp_payment_failed,
        secondaryActionLabel = secondaryLabel
    )

    data class ExternalReaderCollectPaymentState(
        override val amountWithCurrencyLabel: String,
        override val headerLabel: Int = R.string.card_reader_payment_collect_payment_header,
        override val hintLabel: Int = R.string.card_reader_payment_collect_payment_hint,
        override val onSecondaryActionClicked: (() -> Unit),
    ) : ViewState(
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_collect_payment_state),
        illustration = R.drawable.img_card_reader_available,
        secondaryActionLabel = R.string.cancel,
    ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Collecting"
    }

    data class BuiltInReaderCollectPaymentState(
        override val amountWithCurrencyLabel: String,
        override val headerLabel: Int = R.string.card_reader_payment_collect_payment_header,
        override val hintLabel: Int = R.string.card_reader_payment_collect_payment_built_in_hint,
    ) : ViewState(
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_collect_payment_built_in_state),
        illustration = R.drawable.img_card_reader_tpp_collecting_payment,
    ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Collecting"
    }

    data class ExternalReaderProcessingPaymentState(
        override val amountWithCurrencyLabel: String,
        override val onSecondaryActionClicked: (() -> Unit),
    ) : ViewState(
        hintLabel = R.string.card_reader_payment_processing_payment_hint,
        headerLabel = R.string.card_reader_payment_processing_payment_header,
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_processing_payment_state),
        illustration = R.drawable.img_card_reader_available,
        secondaryActionLabel = R.string.cancel,
    ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Processing"
    }

    data class BuiltInReaderProcessingPaymentState(
        override val amountWithCurrencyLabel: String,
    ) : ViewState(
        hintLabel = R.string.card_reader_payment_processing_payment_hint,
        headerLabel = R.string.card_reader_payment_processing_payment_header,
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_processing_payment_state),
        illustration = R.drawable.img_card_reader_tpp_collecting_payment,
    ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Processing"
    }

    data class ExternalReaderCapturingPaymentState(override val amountWithCurrencyLabel: String) :
        ViewState(
            hintLabel = R.string.card_reader_payment_capturing_payment_hint,
            headerLabel = R.string.card_reader_payment_capturing_payment_header,
            paymentStateLabel = UiStringRes(R.string.card_reader_payment_capturing_payment_state),
            illustration = R.drawable.img_card_reader_available
        ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Capturing"
    }

    data class BuiltInReaderCapturingPaymentState(override val amountWithCurrencyLabel: String) :
        ViewState(
            hintLabel = R.string.card_reader_payment_capturing_payment_hint,
            headerLabel = R.string.card_reader_payment_capturing_payment_header,
            paymentStateLabel = UiStringRes(R.string.card_reader_payment_capturing_payment_state),
            illustration = R.drawable.img_card_reader_tpp_collecting_payment
        ),
        PaymentFlow {
        override val nameForTracking: String
            get() = "Capturing"
    }

    data class ExternalReaderPaymentSuccessfulState(
        override val amountWithCurrencyLabel: String,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit),
        override val onTertiaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = R.drawable.img_celebration,
        primaryActionLabel = R.string.card_reader_payment_print_receipt,
        secondaryActionLabel = R.string.card_reader_payment_send_receipt,
        tertiaryActionLabel = R.string.card_reader_payment_save_for_later,
    )

    data class BuiltInReaderPaymentSuccessfulState(
        override val amountWithCurrencyLabel: String,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit),
        override val onTertiaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = R.drawable.img_card_reader_tpp_successful_payment,
        primaryActionLabel = R.string.card_reader_payment_print_receipt,
        secondaryActionLabel = R.string.card_reader_payment_send_receipt,
        tertiaryActionLabel = R.string.card_reader_payment_save_for_later,
    )

    data class ExternalReaderPaymentSuccessfulReceiptSentAutomaticallyState(
        override val amountWithCurrencyLabel: String,
        override val receiptSentAutomaticallyHint: UiString,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onTertiaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = R.drawable.img_celebration,
        primaryActionLabel = R.string.card_reader_payment_print_receipt,
        tertiaryActionLabel = R.string.card_reader_payment_save_for_later,
    )

    data class BuiltInReaderPaymentSuccessfulReceiptSentAutomaticallyState(
        override val amountWithCurrencyLabel: String,
        override val receiptSentAutomaticallyHint: UiString,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onTertiaryActionClicked: (() -> Unit)
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = R.drawable.img_card_reader_tpp_successful_payment,
        primaryActionLabel = R.string.card_reader_payment_print_receipt,
        tertiaryActionLabel = R.string.card_reader_payment_save_for_later,
    )

    data class PrintingReceiptState(
        override val amountWithCurrencyLabel: String,
    ) : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = null,
        primaryActionLabel = null,
        secondaryActionLabel = null,
    ) {
        override val isProgressVisible = true
    }

    object SharingReceiptState : ViewState(
        headerLabel = R.string.card_reader_payment_completed_payment_header,
        illustration = null,
        primaryActionLabel = null,
        secondaryActionLabel = null,
    ) {
        override val isProgressVisible = true
    }

    object ReFetchingOrderState : ViewState(
        headerLabel = R.string.card_reader_payment_fetch_order_loading_header,
        hintLabel = R.string.card_reader_payment_fetch_order_loading_hint,
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_fetch_order_loading_payment_state),
        isProgressVisible = true
    )

    /**********************************************************
     * Interac Refund UI States
     **********************************************************/

    data class RefundLoadingDataState(override val onSecondaryActionClicked: (() -> Unit)) :
        ViewState(
            headerLabel = R.string.card_reader_interac_refund_refund_loading_header,
            hintLabel = R.string.card_reader_payment_collect_payment_loading_hint,
            paymentStateLabel = UiStringRes(R.string.card_reader_payment_collect_payment_loading_payment_state),
            isProgressVisible = true,
            secondaryActionLabel = R.string.cancel,
        ),
        InteracRefundFlow {
        override val nameForTracking: String
            get() = "Loading"
    }

    data class FailedRefundState(
        private val errorType: InteracRefundFlowError,
        override val amountWithCurrencyLabel: String?,
        private val primaryLabel: Int? = R.string.try_again,
        private val secondaryLabel: Int? = null,
        override val onPrimaryActionClicked: (() -> Unit),
        override val onSecondaryActionClicked: (() -> Unit)? = null,
    ) : ViewState(
        headerLabel = R.string.card_reader_interac_refund_refund_failed_header,
        paymentStateLabel = errorType.message,
        paymentStateLabelTopMargin = R.dimen.major_100,
        primaryActionLabel = primaryLabel,
        illustration = R.drawable.img_products_error,
        secondaryActionLabel = secondaryLabel,
    )

    data class CollectRefundState(
        override val amountWithCurrencyLabel: String,
        override val hintLabel: Int = R.string.card_reader_interac_refund_refund_payment_hint,
        override val headerLabel: Int = R.string.card_reader_interac_refund_refund_payment,
        override val onSecondaryActionClicked: (() -> Unit),
    ) : ViewState(
        paymentStateLabel = UiStringRes(R.string.card_reader_payment_collect_payment_state),
        illustration = R.drawable.img_card_reader_available,
        secondaryActionLabel = R.string.cancel,
    ),
        InteracRefundFlow {
        override val nameForTracking: String
            get() = "Collecting"
    }

    data class ProcessingRefundState(override val amountWithCurrencyLabel: String) :
        ViewState(
            hintLabel = R.string.card_reader_payment_processing_payment_hint,
            headerLabel = R.string.card_reader_interac_refund_refund_payment,
            paymentStateLabel = UiStringRes(R.string.card_reader_interac_refund_refund_processing_state),
            illustration = R.drawable.img_card_reader_available
        ),
        InteracRefundFlow {
        override val nameForTracking: String
            get() = "Processing"
    }

    data class RefundSuccessfulState(
        override val amountWithCurrencyLabel: String
    ) : ViewState(
        headerLabel = R.string.card_reader_interac_refund_refund_completed_header,
        illustration = R.drawable.img_celebration,
    )
}

sealed class PaymentFlowError(val message: UiString) {
    object FetchingOrderFailed : PaymentFlowError(UiStringRes(R.string.order_error_fetch_generic))
    object NoNetwork : PaymentFlowError(UiStringRes(R.string.card_reader_payment_failed_no_network_state))
    object Server : PaymentFlowError(UiStringRes(R.string.card_reader_payment_failed_server_error_state))
    object Generic : PaymentFlowError(UiStringRes(R.string.card_reader_payment_failed_unexpected_error_state))
    object Canceled : PaymentFlowError(UiStringRes(R.string.card_reader_payment_failed_canceled))
    data class AmountTooSmall(val errorMessage: UiString) : Declined(errorMessage), NonRetryableError

    object Unknown : Declined(UiStringRes(R.string.card_reader_payment_failed_unknown)), ContactSupportError
    sealed class Declined(message: UiString) : PaymentFlowError(message) {
        object Temporary : Declined(UiStringRes(R.string.card_reader_payment_failed_temporary))
        object Fraud : Declined(UiStringRes(R.string.card_reader_payment_failed_fraud)), NonRetryableError
        object Generic : Declined(UiStringRes(R.string.card_reader_payment_failed_generic)), ContactSupportError
        object InvalidAccount :
            Declined(UiStringRes(R.string.card_reader_payment_failed_invalid_account)),
            NonRetryableError

        object CardNotSupported : Declined(UiStringRes(R.string.card_reader_payment_failed_card_not_supported))
        object CurrencyNotSupported :
            Declined(UiStringRes(R.string.card_reader_payment_failed_currency_not_supported)), NonRetryableError

        object DuplicateTransaction :
            Declined(UiStringRes(R.string.card_reader_payment_failed_duplicate_transaction)), NonRetryableError

        object ExpiredCard : Declined(UiStringRes(R.string.card_reader_payment_failed_expired_card))
        object IncorrectPostalCode :
            Declined(UiStringRes(R.string.card_reader_payment_failed_incorrect_postal_code)), NonRetryableError

        object InsufficientFunds : Declined(UiStringRes(R.string.card_reader_payment_failed_insufficient_funds))
        object InvalidAmount :
            Declined(UiStringRes(R.string.card_reader_payment_failed_invalid_amount)),
            NonRetryableError

        object PinRequired : Declined(UiStringRes(R.string.card_reader_payment_failed_pin_required))
        object IncorrectPin : Declined(UiStringRes(R.string.card_reader_payment_failed_incorrect_pin))
        object TooManyPinTries : Declined(UiStringRes(R.string.card_reader_payment_failed_too_many_pin_tries))
        object TestCard : Declined(UiStringRes(R.string.card_reader_payment_failed_test_card))
        object TestModeLiveCard : Declined(UiStringRes(R.string.card_reader_payment_failed_test_mode_live_card))
    }

    sealed class BuiltInReader(@StringRes message: Int) : PaymentFlowError(UiStringRes(message)) {
        object NfcDisabled : BuiltInReader(R.string.card_reader_payment_failed_nfc_disabled)
        object DeviceIsNotSupported :
            BuiltInReader(R.string.card_reader_payment_failed_device_is_not_supported),
            ContactSupportError

        object InvalidAppSetup :
            BuiltInReader(R.string.card_reader_payment_failed_app_setup_is_invalid),
            ContactSupportError

        object AppKilledWhileInBackground :
            BuiltInReader(R.string.card_reader_payment_vm_killed_when_tpp_in_foreground),
            ContactSupportError

        object PinRequired :
            Declined(UiStringRes(R.string.card_reader_payment_failed_pin_required_tap_to_pay)),
            PurchaseHardwareReaderError
    }

    interface NonRetryableError

    interface ContactSupportError

    interface PurchaseHardwareReaderError
}

sealed class InteracRefundFlowError(val message: UiString) {
    object FetchingOrderFailed : InteracRefundFlowError(UiStringRes(R.string.order_error_fetch_generic))
    object NoNetwork :
        InteracRefundFlowError(UiStringRes(R.string.card_reader_payment_failed_no_network_state))
    object Server : InteracRefundFlowError(UiStringRes(R.string.card_reader_payment_failed_server_error_state))
    object Generic : InteracRefundFlowError(
        UiStringRes(R.string.card_reader_interac_refund_refund_failed_unexpected_error_state)
    )
    object NonRetryableGeneric :
        InteracRefundFlowError(UiStringRes(R.string.card_reader_interac_refund_refund_failed_unexpected_error_state)),
        NonRetryableError

    object Cancelled : InteracRefundFlowError(UiStringRes(R.string.card_reader_interac_refund_refund_failed_cancelled))
    object Unknown : Declined(R.string.card_reader_interac_refund_refund_failed_header), ContactSupportError
    sealed class Declined(@StringRes message: Int) : InteracRefundFlowError(UiStringRes(message)) {
        object Temporary : Declined(R.string.card_reader_payment_failed_temporary)
        object Fraud : Declined(R.string.card_reader_interac_refund_refund_failed_fraud), NonRetryableError
        object Generic : Declined(R.string.card_reader_interac_refund_refund_failed_generic), ContactSupportError
        object InvalidAccount : Declined(R.string.card_reader_payment_failed_invalid_account), NonRetryableError
        object CardNotSupported : Declined(R.string.card_reader_interac_refund_refund_failed_card_not_supported)
        object CurrencyNotSupported :
            Declined(R.string.card_reader_payment_failed_currency_not_supported), NonRetryableError

        object DuplicateTransaction :
            Declined(R.string.card_reader_interac_refund_refund_failed_duplicate_transaction), NonRetryableError

        object ExpiredCard : Declined(R.string.card_reader_payment_failed_expired_card)
        object IncorrectPostalCode :
            Declined(R.string.card_reader_payment_failed_incorrect_postal_code), NonRetryableError

        object InsufficientFunds : Declined(R.string.card_reader_interac_refund_refund_failed_insufficient_funds)
        object InvalidAmount :
            Declined(R.string.card_reader_interac_refund_refund_failed_invalid_amount), NonRetryableError

        object PinRequired : Declined(R.string.card_reader_payment_failed_pin_required)
        object TooManyPinTries : Declined(R.string.card_reader_payment_failed_too_many_pin_tries)
        object TestCard : Declined(R.string.card_reader_interac_refund_refund_failed_test_card)
        object TestModeLiveCard : Declined(R.string.card_reader_payment_failed_test_mode_live_card)
    }

    interface NonRetryableError

    interface ContactSupportError
}
