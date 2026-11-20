package com.woocommerce.android.cardreader.remote

import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardReaderRemoteErrorTest {
    @Test
    fun `given a known code, when parsed, then the matching error is returned`() {
        // WHEN
        val error = CardReaderRemoteError.fromCode("phone_not_eligible")

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.PhoneNotEligible)
    }

    @Test
    fun `given a code from another app version, when parsed, then it is kept as unknown`() {
        // WHEN
        val error = CardReaderRemoteError.fromCode("shipped_in_a_later_version")

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.Unknown("shipped_in_a_later_version"))
    }

    @Test
    fun `given a tap to pay unsupported failure, when classified, then the phone is not eligible`() {
        // GIVEN
        val exception: TerminalException = mock()
        whenever(exception.errorCode).thenReturn(TerminalErrorCode.TAP_TO_PAY_UNSUPPORTED_DEVICE)

        // WHEN
        val error = exception.toCardReaderRemoteError(CardReaderRemoteError.ConnectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.PhoneNotEligible)
    }

    @Test
    fun `given an insecure environment failure, when classified, then the insecure environment error is returned`() {
        // GIVEN
        val exception: TerminalException = mock()
        whenever(exception.errorCode).thenReturn(TerminalErrorCode.TAP_TO_PAY_INSECURE_ENVIRONMENT)

        // WHEN
        val error = exception.toCardReaderRemoteError(CardReaderRemoteError.CollectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.InsecureEnvironment)
    }

    @Test
    fun `given nfc is disabled, when classified, then the nfc error is returned`() {
        // GIVEN
        val exception: TerminalException = mock()
        whenever(exception.errorCode).thenReturn(TerminalErrorCode.TAP_TO_PAY_NFC_DISABLED)

        // WHEN
        val error = IllegalStateException("wrapped", exception)
            .toCardReaderRemoteError(CardReaderRemoteError.CollectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.NfcDisabled)
    }

    @Test
    fun `given an expired session, when classified, then the token is reported as invalid`() {
        // GIVEN
        val exception: TerminalException = mock()
        whenever(exception.errorCode).thenReturn(TerminalErrorCode.SESSION_EXPIRED)

        // WHEN
        val error = exception.toCardReaderRemoteError(CardReaderRemoteError.ConnectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.TokenInvalid)
    }

    @Test
    fun `given an unrecognised failure, when classified, then the fallback is returned`() {
        // WHEN
        val error = IllegalStateException("boom").toCardReaderRemoteError(CardReaderRemoteError.CollectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.CollectFailed)
    }

    @Test
    fun `given the new collect codes, when parsed, then the matching errors are returned`() {
        // WHEN
        val canceled = CardReaderRemoteError.fromCode("canceled")
        val timedOut = CardReaderRemoteError.fromCode("card_read_timed_out")

        // THEN
        assertThat(canceled).isEqualTo(CardReaderRemoteError.Canceled)
        assertThat(timedOut).isEqualTo(CardReaderRemoteError.CardReadTimedOut)
    }

    @Test
    fun `given the card read is canceled, when classified as a collect failure, then canceled is returned`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.CANCELED)

        // WHEN
        val error = exception.toCollectRemoteError()

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.Canceled)
    }

    @Test
    fun `given the reader cancels the card read, when classified as a collect failure, then canceled is returned`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.CANCELED_BY_READER)

        // WHEN
        val error = IllegalStateException("wrapped", exception).toCollectRemoteError()

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.Canceled)
    }

    @Test
    fun `given the card read times out, when classified as a collect failure, then card read timed out is returned`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.CARD_READ_TIMED_OUT)

        // WHEN
        val error = exception.toCollectRemoteError()

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.CardReadTimedOut)
    }

    @Test
    fun `given nfc is disabled, when classified as a collect failure, then the nfc error is returned`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.TAP_TO_PAY_NFC_DISABLED)

        // WHEN
        val error = exception.toCollectRemoteError()

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.NfcDisabled)
    }

    @Test
    fun `given an unrecognised failure, when classified as a collect failure, then collect failed is returned`() {
        // WHEN
        val error = IllegalStateException("boom").toCollectRemoteError()

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.CollectFailed)
    }

    @Test
    fun `given a canceled failure while connecting, when classified, then the connect fallback is kept`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.CANCELED)

        // WHEN
        val error = exception.toCardReaderRemoteError(CardReaderRemoteError.ConnectFailed)

        // THEN
        assertThat(error).isEqualTo(CardReaderRemoteError.ConnectFailed)
    }

    @Test
    fun `given a wrapped stripe failure, when the stripe code is read, then the terminal error code name is returned`() {
        // GIVEN
        val exception = aTerminalException(TerminalErrorCode.CARD_READ_TIMED_OUT)

        // WHEN
        val code = IllegalStateException("wrapped", exception).stripeErrorCode()

        // THEN
        assertThat(code).isEqualTo("CARD_READ_TIMED_OUT")
    }

    @Test
    fun `given a failure without a stripe cause, when the stripe code is read, then it is null`() {
        // WHEN
        val code = IllegalStateException("boom").stripeErrorCode()

        // THEN
        assertThat(code).isNull()
    }

    private fun aTerminalException(code: TerminalErrorCode): TerminalException = mock {
        on { errorCode } doReturn code
    }
}
