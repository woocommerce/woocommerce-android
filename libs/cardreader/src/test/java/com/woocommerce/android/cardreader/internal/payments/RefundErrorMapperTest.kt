package com.woocommerce.android.cardreader.internal.payments

import com.stripe.stripeterminal.external.models.TerminalException
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.Cancelled
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.DeclinedByBackendError
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.Generic
import com.woocommerce.android.cardreader.payments.CardInteracRefundStatus.RefundStatusErrorType.NoNetwork
import com.woocommerce.android.cardreader.payments.RefundParams
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

@RunWith(MockitoJUnitRunner::class)
class RefundErrorMapperTest {
    private lateinit var mapper: RefundErrorMapper

    private val refundParameters: RefundParams = mock()

    private val terminalException = mock<TerminalException>().also {
        whenever(it.errorCode).thenReturn(TerminalException.TerminalErrorCode.DECLINED_BY_READER)
        whenever(it.errorMessage).thenReturn("Dummy error message")
    }

    @Before
    fun setUp() {
        mapper = RefundErrorMapper()
    }

    @Test
    fun `when Terminal exception thrown, then refund params is used`() {
        whenever(terminalException.errorCode).thenReturn(TerminalException.TerminalErrorCode.DECLINED_BY_STRIPE_API)
        val refundParams = RefundParams(
            chargeId = "",
            amount = BigDecimal.TEN,
            currency = "USD"
        )
        val result = mapper.mapTerminalError(refundParams, terminalException)

        Assertions.assertThat((result.refundParams)).isEqualTo(refundParams)
    }

    @Test
    fun `when PAYMENT_DECLINED_BY_STRIPE_API Terminal exception thrown, then PAYMENT_DECLINED type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalException.TerminalErrorCode.DECLINED_BY_STRIPE_API)

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `when other Terminal exception thrown, then GENERIC_ERROR type returned`() {
        whenever(terminalException.errorCode).thenReturn(mock())

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type).isEqualTo(Generic)
    }

    @Test
    fun `when STRIPE_API_CONNECTION_ERROR Terminal exception thrown, then NO_NETWORK type returned`() {
        whenever(terminalException.errorCode).thenReturn(
            TerminalException.TerminalErrorCode.STRIPE_API_CONNECTION_ERROR
        )

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type).isEqualTo(NoNetwork)
    }

    @Test
    fun `when CANCELLED Terminal exception thrown, then CANCELLED type returned`() {
        whenever(terminalException.errorCode).thenReturn(
            TerminalException.TerminalErrorCode.CANCELED
        )

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type).isEqualTo(Cancelled)
    }

    @Test
    fun `when PAYMENT_DECLINED Terminal exception throw with null code, then unknown type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalException.TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `given card_declined with approve_with_id, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("approve_with_id")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with issuer_not_avail, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("issuer_not_available")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with processing_error, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("processing_error")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with reenter_transaction, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("reenter_transaction")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with try_again_later, when terminal exception thrown, then temporary type returned`() {
        setupStripeApiCardDeclined("try_again_later")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Temporary)
    }

    @Test
    fun `given card_declined with call_issuer, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("call_issuer")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with card_velocity_exceeded, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("card_velocity_exceeded")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with do_not_honor, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("do_not_honor")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with do_not_try_again, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("do_not_try_again")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with fraudulent, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("fraudulent")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with lost_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("lost_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with merchant_blacklist, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("merchant_blacklist")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with pickup_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("pickup_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with restricted_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("restricted_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with rev_of_all_auth, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("revocation_of_all_authorizations")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with revocation_of_auth, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("revocation_of_authorization")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with security_violation, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("security_violation")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with stolen_card, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("stolen_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with stop_payment_order, when terminal exception thrown, then fraud type returned`() {
        setupStripeApiCardDeclined("stop_payment_order")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Fraud)
    }

    @Test
    fun `given card_declined with generic_decline, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("generic_decline")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with no_action_taken, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("no_action_taken")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with not_permitted, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("not_permitted")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with service_not_allowed, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("service_not_allowed")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with trans_not_allowed, when terminal exception thrown, then generic type returned`() {
        setupStripeApiCardDeclined("transaction_not_allowed")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.Generic)
    }

    @Test
    fun `given card_declined with invalid_account, when terminal exception thrown, then inv account type returned`() {
        setupStripeApiCardDeclined("invalid_account")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAccount)
    }

    @Test
    fun `given card_declined with new_acc_info_avl, when terminal exception thrown, then inv account type returned`() {
        setupStripeApiCardDeclined("new_account_information_available")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAccount)
    }

    @Test
    fun `given card_declined with card_not_sup, when terminal exception thrown, then card not supp type returned`() {
        setupStripeApiCardDeclined("card_not_supported")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.CardNotSupported)
    }

    @Test
    fun `given card_declined with currency_not_sup, when terminal exception thrown, then curr not sup type returned`() {
        setupStripeApiCardDeclined("currency_not_supported")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.CurrencyNotSupported)
    }

    @Test
    fun `given card_declined with dupl_transaction, when terminal exception thrown, then dupl trans type returned`() {
        setupStripeApiCardDeclined("duplicate_transaction")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.DuplicateTransaction)
    }

    @Test
    fun `given card_declined with expired_card, when terminal exception thrown, then expired card type returned`() {
        setupStripeApiCardDeclined("expired_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.ExpiredCard)
    }

    @Test
    fun `given card_declined with incorrect_zip, when terminal exception thrown, then incor zip type returned`() {
        setupStripeApiCardDeclined("incorrect_zip")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.IncorrectPostalCode)
    }

    @Test
    fun `given card_declined with insufficient_funds, when terminal exception thrown, then ins funds type returned`() {
        setupStripeApiCardDeclined("insufficient_funds")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.InsufficientFunds)
    }

    @Test
    fun `given card_declined with with_count_exc, when terminal exception thrown, then ins funds type returned`() {
        setupStripeApiCardDeclined("withdrawal_count_limit_exceeded")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.InsufficientFunds)
    }

    @Test
    fun `given card_declined with invalid_amount, when terminal exception thrown, then inv amount type returned`() {
        setupStripeApiCardDeclined("invalid_amount")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.InvalidAmount)
    }

    @Test
    fun `given card_declined with invalid_pin, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("invalid_pin")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with offline_pin_required, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("offline_pin_required")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with online_or_off_pin_req, when terminal exception thrown, then pin req type returned`() {
        setupStripeApiCardDeclined("online_or_offline_pin_required")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.PinRequired)
    }

    @Test
    fun `given card_declined with pin_try_exceeded, when terminal exception thrown, then too many pin type returned`() {
        setupStripeApiCardDeclined("pin_try_exceeded")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.TooManyPinTries)
    }

    @Test
    fun `given card_declined with testmode_decline, when terminal exception thrown, then test card type returned`() {
        setupStripeApiCardDeclined("testmode_decline")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.TestCard)
    }

    @Test
    fun `given card_declined with test_live_card, when terminal exception thrown, then test live card type returned`() {
        setupStripeApiCardDeclined("test_mode_live_card")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.CardDeclined.TestModeLiveCard)
    }

    @Test
    fun `given card_declined with random_string, when terminal exception thrown, then unknown type returned`() {
        setupStripeApiCardDeclined("random_string")

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.Unknown)
    }

    @Test
    fun `given card_declined with null, when terminal exception thrown, then unknown type returned`() {
        setupStripeApiCardDeclined(null)

        val result = mapper.mapTerminalError(refundParameters, terminalException)

        Assertions.assertThat(result.type)
            .isEqualTo(DeclinedByBackendError.Unknown)
    }

    private fun setupStripeApiCardDeclined(declineCode: String?) {
        whenever(terminalException.errorCode).thenReturn(TerminalException.TerminalErrorCode.DECLINED_BY_STRIPE_API)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.declineCode).thenReturn(declineCode)
    }
}
