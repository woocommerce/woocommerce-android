package com.woocommerce.android.cardreader.remote

import com.stripe.stripeterminal.external.models.TerminalErrorCode
import com.stripe.stripeterminal.external.models.TerminalException
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
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
}
