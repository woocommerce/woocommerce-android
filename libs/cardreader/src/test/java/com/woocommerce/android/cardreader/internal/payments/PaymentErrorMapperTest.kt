package com.woocommerce.android.cardreader.internal.payments

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import com.stripe.stripeterminal.model.external.PaymentIntent
import com.stripe.stripeterminal.model.external.TerminalException
import com.stripe.stripeterminal.model.external.TerminalException.TerminalErrorCode
import com.stripe.stripeterminal.model.external.TerminalException.TerminalErrorCode.PAYMENT_DECLINED_BY_READER
import com.woocommerce.android.cardreader.CardPaymentStatus.CardPaymentStatusErrorType.*
import com.woocommerce.android.cardreader.CardReaderStore.CapturePaymentResponse
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class PaymentErrorMapperTest {
    private lateinit var mapper: PaymentErrorMapper

    private val terminalException = mock<TerminalException>().also {
        whenever(it.errorCode).thenReturn(PAYMENT_DECLINED_BY_READER)
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

        assertThat(result.type).isEqualTo(CARD_READ_TIMED_OUT)
    }

    @Test
    fun `when PAYMENT_DECLINED_BY_STRIPE_API Terminal exception thrown, then PAYMENT_DECLINED type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.PAYMENT_DECLINED_BY_STRIPE_API)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(PAYMENT_DECLINED)
    }

    @Test
    fun `when REQUEST_TIMED_OUT Terminal exception thrown, then NO_NETWORK type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.REQUEST_TIMED_OUT)

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(NO_NETWORK)
    }

    @Test
    fun `when other Terminal exception thrown, then GENERIC_ERROR type returned`() {
        whenever(terminalException.errorCode).thenReturn(mock())

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(GENERIC_ERROR)
    }

    @Test
    fun `when STRIPE_API_ERROR with amount_too_small code is thrown, then AMOUNT_TOO_SMALL type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.STRIPE_API_ERROR)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn(
            PaymentErrorMapper.StripeApiError.AMOUNT_TOO_SMALL.message
        )

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(AMOUNT_TOO_SMALL)
    }

    @Test
    fun `when STRIPE_API_ERROR with code other than amount_too_small is thrown, then GENERIC_ERROR type returned`() {
        whenever(terminalException.errorCode).thenReturn(TerminalErrorCode.STRIPE_API_ERROR)
        whenever(terminalException.apiError).thenReturn(mock())
        whenever(terminalException.apiError?.code).thenReturn("error")

        val result = mapper.mapTerminalError(mock(), terminalException)

        assertThat(result.type).isEqualTo(GENERIC_ERROR)
    }

    @Test
    fun `when NETWORK_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.NetworkError)

        assertThat(result.type).isEqualTo(NO_NETWORK)
    }

    @Test
    fun `when GENERIC_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.GenericError)

        assertThat(result.type).isEqualTo(GENERIC_ERROR)
    }

    @Test
    fun `when MISSING_ORDER capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.MissingOrder)

        assertThat(result.type).isEqualTo(GENERIC_ERROR)
    }

    @Test
    fun `when CAPTURE_ERROR capture payment exception thrown, then NO_NETWORK type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.CaptureError)

        assertThat(result.type).isEqualTo(GENERIC_ERROR)
    }

    @Test
    fun `when SERVER_ERROR capture payment exception thrown, then SERVER_ERROR type returned`() {
        val result = mapper.mapCapturePaymentError(mock(), CapturePaymentResponse.Error.ServerError)

        assertThat(result.type).isEqualTo(SERVER_ERROR)
    }
}
