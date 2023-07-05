package com.woocommerce.android.ui.payments.cardreader.hub

import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class CardReaderHubTapToPayUnavailableHandlerTest {
    private val triggerEvent: (MultiLiveEvent.Event) -> Unit = mock()
    private val positiveButtonClick: (CardReaderHubTapToPayUnavailableHandler.ActionType) -> Unit = mock()
    private val handler = CardReaderHubTapToPayUnavailableHandler()

    @Test
    fun `given NfcNotAvailable, when handleTTPUnavailable, then triggers showdialog event`() {
        // GIVEN
        val status = TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable
        val captor = argumentCaptor<MultiLiveEvent.Event.ShowDialog>()

        // WHEN
        handler.handleTTPUnavailable(
            status = status,
            triggerEvent = triggerEvent,
            positiveButtonClick = positiveButtonClick
        )

        // THEN
        verify(triggerEvent).invoke(captor.capture())

        assertThat(captor.firstValue.titleId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_title)
        assertThat(captor.firstValue.messageId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_nfc)
        assertThat(captor.firstValue.positiveButtonId).isEqualTo(R.string.card_reader_upsell_card_reader_banner_cta)
        assertThat(captor.firstValue.negativeButtonId).isEqualTo(R.string.close)
    }

    @Test
    fun `given SystemVersionNotSupported, when handleTTPUnavailable, then triggers showdialog event`() {
        // GIVEN
        val status = TapToPayAvailabilityStatus.Result.NotAvailable.SystemVersionNotSupported
        val captor = argumentCaptor<MultiLiveEvent.Event.ShowDialog>()

        // WHEN
        handler.handleTTPUnavailable(
            status = status,
            triggerEvent = triggerEvent,
            positiveButtonClick = positiveButtonClick
        )

        // THEN
        verify(triggerEvent).invoke(captor.capture())
        assertThat(captor.firstValue.titleId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_title)
        assertThat(captor.firstValue.messageId).isEqualTo(
            R.string.card_reader_tap_to_pay_not_available_error_android_version
        )
        assertThat(captor.firstValue.positiveButtonId).isEqualTo(R.string.card_reader_upsell_card_reader_banner_cta)
        assertThat(captor.firstValue.negativeButtonId).isEqualTo(R.string.close)
    }

    @Test
    fun `given CountryNotSupported, when handleTTPUnavailable, then triggers showdialog event`() {
        // GIVEN
        val status = TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported
        val captor = argumentCaptor<MultiLiveEvent.Event.ShowDialog>()

        // WHEN
        handler.handleTTPUnavailable(
            status = status,
            triggerEvent = triggerEvent,
            positiveButtonClick = positiveButtonClick
        )

        // THEN
        verify(triggerEvent).invoke(captor.capture())
        assertThat(captor.firstValue.titleId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_title)
        assertThat(captor.firstValue.messageId).isEqualTo(
            R.string.card_reader_tap_to_pay_not_available_error_country
        )
        assertThat(captor.firstValue.positiveButtonId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_check_requirements_button)
        assertThat(captor.firstValue.negativeButtonId).isEqualTo(R.string.close)
    }

    @Test
    fun `given GooglePlayServicesNotAvailable, when handleTTPUnavailable, then triggers showdialog event`() {
        // GIVEN
        val status = TapToPayAvailabilityStatus.Result.NotAvailable.GooglePlayServicesNotAvailable
        val captor = argumentCaptor<MultiLiveEvent.Event.ShowDialog>()

        // WHEN
        handler.handleTTPUnavailable(
            status = status,
            triggerEvent = triggerEvent,
            positiveButtonClick = positiveButtonClick
        )

        // THEN
        verify(triggerEvent).invoke(captor.capture())
        assertThat(captor.firstValue.titleId).isEqualTo(R.string.card_reader_tap_to_pay_not_available_error_title)
        assertThat(captor.firstValue.messageId).isEqualTo(
            R.string.card_reader_tap_to_pay_not_available_error_gms
        )
        assertThat(captor.firstValue.positiveButtonId).isEqualTo(R.string.card_reader_upsell_card_reader_banner_cta)
        assertThat(captor.firstValue.negativeButtonId).isEqualTo(R.string.close)
    }

    @Test
    fun `given TapToPayNotEnabled, when handleTTPUnavailable, then triggers showtoast event`() {
        // GIVEN
        val status = TapToPayAvailabilityStatus.Result.NotAvailable.TapToPayDisabled

        // WHEN
        handler.handleTTPUnavailable(
            status = status,
            triggerEvent = triggerEvent,
            positiveButtonClick = positiveButtonClick
        )

        // THEN
        verify(triggerEvent).invoke(
            CardReaderHubViewModel.CardReaderHubEvents.ShowToast(
                R.string.card_reader_tap_to_pay_not_available_error
            )
        )
    }
}
