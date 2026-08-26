package com.woocommerce.android.ui.woopos.cardreader.remote

import com.woocommerce.android.R
import com.woocommerce.android.cardreader.remote.CardReaderRemoteError
import com.woocommerce.android.viewmodel.ResourceProvider
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosRemoteReaderErrorMapperTest {
    private val resourceProvider: ResourceProvider = mock()
    private val mapper = WooPosRemoteReaderErrorMapper(resourceProvider)

    @Test
    fun `given a mapped error, when mapping, then the specific copy is used`() {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_remote_reader_failed_phone_not_eligible))
            .thenReturn("not eligible")

        // WHEN
        val message = mapper.toUserMessage(
            error = CardReaderRemoteError.PhoneNotEligible,
            fallback = R.string.woopos_remote_reader_connect_failed_generic,
        )

        // THEN
        assertThat(message).isEqualTo("not eligible")
    }

    @Test
    fun `given an nfc disabled error, when mapping, then the actionable copy is used`() {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_remote_reader_failed_nfc_disabled))
            .thenReturn("turn on nfc")

        // WHEN
        val message = mapper.toUserMessage(
            error = CardReaderRemoteError.NfcDisabled,
            fallback = R.string.woopos_remote_reader_connect_failed_generic,
        )

        // THEN
        assertThat(message).isEqualTo("turn on nfc")
    }

    @Test
    fun `given an unknown code from another app version, when mapping, then the fallback is used`() {
        // GIVEN
        whenever(resourceProvider.getString(R.string.woopos_remote_payment_failed_generic))
            .thenReturn("something went wrong")

        // WHEN
        val message = mapper.toUserMessage(
            error = CardReaderRemoteError.Unknown("shipped_in_a_later_version"),
            fallback = R.string.woopos_remote_payment_failed_generic,
        )

        // THEN
        assertThat(message).isEqualTo("something went wrong")
    }
}
