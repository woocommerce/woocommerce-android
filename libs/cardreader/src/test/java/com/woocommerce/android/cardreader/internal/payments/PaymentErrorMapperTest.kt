package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.PaymentIntent
import com.stripe.stripeterminal.external.models.TerminalException
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException.TerminalErrorCode.DECLINED_BY_READER
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import com.woocommerce.android.cardreader.internal.CardReaderBaseUnitTest
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.BuiltInReader
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Canceled
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.CardReadTimeOut
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.CardPaymentStatus.CardPaymentStatusErrorType.Server
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class PaymentErrorMapperTest : CardReaderBaseUnitTest() {
    private lateinit var mapper: PaymentErrorMapper

    private val terminalException = mock<TerminalException>().also {
        whenever(it.errorCode).thenReturn(DECLINED_BY_READER)
        whenever(it.errorMessage).thenReturn("Dummy error message")
    }

    @Before
    fun setUp() {
        mapper = PaymentErrorMapper()
    }

    @Test
    fun `when exception contains payment intent, then this payment intent is used`() {
        val testingPaymentIntent = mock<PaymentIntent>()
        val originalPaymentIntent = mock<PaymentIntent>()
        whenever(terminalException.paymentIntent).thenReturn(testingPaymentIntent)

        val result = mapper.mapTerminalError(originalPaymentIntent, terminalException)

        assertThat((result.paymentDataForRetry as PaymentDataImpl).paymentIntent).isEqualTo(testingPaymentIntent)
    }

    @Test
    fun `when exception does NOT contain payment intent, then original payment intent is used`() {
        val testingPaymentIntent = null
        val originalPaymentIntent = mock<PaymentIntent>()
        whenever(terminalException.paymentIntent).thenReturn(testingPaymentIntent)

        val result = mapper.mapTerminalError(originalPaymentIntent, terminalException)

        assertThat((result.paymentDataForRetry as PaymentDataImpl).paymentIntent).isEqualTo(originalPaymentIntent)
    }

    @Test
    fun `when CARD_READ_TIMED_OUT Terminal exception thrown, then CARD_READ_TIMED_OUT type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.CARD_READ_TIMED_OUT)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(CardReadTimeOut)
    }

    @Test
    fun `when PAYMENT_DECLINED_BY_STRIPE_API Terminal exception thrown, then PAYMENT_DECLINED type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.DECLINED_BY_STRIPE_API)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `when REQUEST_TIMED_OUT Terminal exception thrown, then NO_NETWORK type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.REQUEST_TIMED_OUT)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(NoNetwork)
    }

    @Test
    fun `when other Terminal exception thrown, then GENERIC_ERROR type returned`() {
        whenever(terminalException.errorCode).thenReturn(mock())

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(Generic)
    }

    @Test
    fun `when NETWORK_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.NetworkError("error message"))

        assertThat(result.type).isEqualTo(NoNetwork)
        assertThat(result.errorMessage).isEqualTo("Capturing payment failed: NetworkError(errorMsg=error message)")
    }

    @Test
    fun `when GENERIC_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.GenericError("error message"))

        assertThat(result.type).isEqualTo(Generic)
        assertThat(result.errorMessage).isEqualTo("Capturing payment failed: GenericError(errorMsg=error message)")
    }

    @Test
    fun `when MISSING_ORDER capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.MissingOrder("error message"))

        assertThat(result.type).isEqualTo(Generic)
        assertThat(result.errorMessage).isEqualTo("Capturing payment failed: MissingOrder(errorMsg=error message)")
    }

    @Test
    fun `when CAPTURE_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.CaptureError("error message"))

        assertThat(result.type).isEqualTo(Generic)
        assertThat(result.errorMessage).isEqualTo("Capturing payment failed: CaptureError(errorMsg=error message)")
    }

    @Test
    fun `when SERVER_ERROR capture payment exception thrown, then SERVER_ERROR type returned`() {
        val result = mapper.mapCapturePaymentError(
            mock(),
            CapturePaymentResponse.Error.ServerError(
                "error message"
            )
        )

        assertThat(result.type).isEqualTo(Server("error message"))
        assertThat(result.errorMessage).isEqualTo("Capturing payment failed: ServerError(errorMsg=error message)")
    }

    @Test
    fun `when AMOUNT_TOO_SMALL Terminal exception thrown, then AmountTooSmall type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn("amount_too_small")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.AmountTooSmall)
    }

    @Test
    fun `when PAYMENT_DECLINED with code other than amount_too_small is thrown, then unknown type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn("error")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `when PAYMENT_DECLINED Terminal exception throw with null code, then unknown type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn(null)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `given card_declined with approve_with_id, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("approve_with_id")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with issuer_not_avail, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("issuer_not_available")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with processing_error, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("processing_error")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with reenter_transaction, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("reenter_transaction")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with try_again_later, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("try_again_later")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with call_issuer, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("call_issuer")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with card_velocity_exceeded, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("card_velocity_exceeded")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with do_not_honor, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("do_not_honor")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with do_not_try_again, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("do_not_try_again")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with fraudulent, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("fraudulent")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with lost_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("lost_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with merchant_blacklist, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("merchant_blacklist")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with pickup_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("pickup_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with restricted_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("restricted_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with rev_of_all_auth, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("revocation_of_all_authorizations")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with revocation_of_auth, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("revocation_of_authorization")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with security_violation, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("security_violation")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with stolen_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("stolen_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with stop_payment_order, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("stop_payment_order")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with generic_decline, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("generic_decline")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with no_action_taken, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("no_action_taken")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with not_permitted, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("not_permitted")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with service_not_allowed, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("service_not_allowed")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with trans_not_allowed, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("transaction_not_allowed")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with invalid_account, when terminal exception thrown, then inv account type returned`() {
        setupStripeApiCardDeclined("invalid_account")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAccount)
    }

    @Test
    fun `given card_declined with new_acc_info_avl, when terminal exception thrown, then inv account type returned`() {
        setupStripeApiCardDeclined("new_account_information_available")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAccount)
    }

    @Test
    fun `given card_declined with card_not_sup, when terminal exception thrown, then card not supp type returned`() {
        setupStripeApiCardDeclined("card_not_supported")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.CardNotSupported)
    }

    @Test
    fun `given card_declined with currency_not_sup, when terminal exception thrown, then curr not sup type returned`() {
        setupStripeApiCardDeclined("currency_not_supported")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.CurrencyNotSupported)
    }

    @Test
    fun `given card_declined with dupl_transaction, when terminal exception thrown, then dupl trans type returned`() {
        setupStripeApiCardDeclined("duplicate_transaction")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.DuplicateTransaction)
    }

    @Test
    fun `given card_declined with expired_card, when terminal exception thrown, then expired card type returned`() {
        setupStripeApiCardDeclined("expired_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.ExpiredCard)
    }

    @Test
    fun `given card_declined with incorrect_zip, when terminal exception thrown, then incor zip type returned`() {
        setupStripeApiCardDeclined("incorrect_zip")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.IncorrectPostalCode)
    }

    @Test
    fun `given card_declined with insufficient_funds, when terminal exception thrown, then ins funds type returned`() {
        setupStripeApiCardDeclined("insufficient_funds")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.InsufficientFunds)
    }

    @Test
    fun `given card_declined with with_count_exc, when terminal exception thrown, then ins funds type returned`() {
        setupStripeApiCardDeclined("withdrawal_count_limit_exceeded")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.InsufficientFunds)
    }

    @Test
    fun `given card_declined with invalid_amount, when terminal exception thrown, then inv amount type returned`() {
        setupStripeApiCardDeclined("invalid_amount")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAmount)
    }

    @Test
    fun `given card_declined with invalid_pin, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("invalid_pin")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with offline_pin_required, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("offline_pin_required")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with online_or_off_pin_req, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("online_or_offline_pin_required")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with incorrect_pin, when terminal exception thrown, then incorrect pin type returned`() {
        setupStripeApiCardDeclined("incorrect_pin")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.IncorrectPin)
    }

    @Test
    fun `given card_declined with pin_try_exceeded, when terminal exception thrown, then too many pin type returned`() {
        setupStripeApiCardDeclined("pin_try_exceeded")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.TooManyPinTries)
    }

    @Test
    fun `given card_declined with testmode_decline, when terminal exception thrown, then test card type returned`() {
        setupStripeApiCardDeclined("testmode_decline")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.TestCard)
    }

    @Test
    fun `given card_declined with test_live_card, when terminal exception thrown, then test live card type returned`() {
        setupStripeApiCardDeclined("test_mode_live_card")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.CardDeclined.TestModeLiveCard)
    }

    @Test
    fun `given card_declined with random_string, when terminal exception thrown, then unknown type returned`() {
        setupStripeApiCardDeclined("random_string")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `given card_declined with null, when terminal exception thrown, then unknown type returned`() {
        setupStripeApiCardDeclined(null)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `given local_mobile_nfc_disabled, when terminal exception thrown, then nfc disabled type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.LOCAL_MOBILE_NFC_DISABLED)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(BuiltInReader.NfcDisabled)
    }

    @Test
    fun `given local_mobile_library_not_included, when terminal exception thrown, then invalid app setup returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.LOCAL_MOBILE_LIBRARY_NOT_INCLUDED)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(BuiltInReader.InvalidAppSetup)
    }

    @Test
    fun `given local_mobile_unsupported_device, when terminal exception thrown, then device unsupported returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.LOCAL_MOBILE_UNSUPPORTED_DEVICE)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(BuiltInReader.DeviceIsNotSupported)
    }

    @Test
    fun `given local_mobile_unsupported_android_version, when terminal exception thrown, then device unsupported`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.LOCAL_MOBILE_UNSUPPORTED_ANDROID_VERSION)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(BuiltInReader.DeviceIsNotSupported)
    }

    @Test
    fun `given local_mobile_device_tampered, when terminal exception thrown, then device unsupported returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.LOCAL_MOBILE_DEVICE_TAMPERED)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(BuiltInReader.DeviceIsNotSupported)
    }

    @Test
    fun `given canceled, when terminal exception thrown, then canceled returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.CANCELED)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(Canceled)
    }

    private fun setupStripeApiCardDeclined(declineCode: String?) {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn("card_declined")
        whenever(terminalException.apiError?.declineCode).thenReturn(declineCode)
    }
}
