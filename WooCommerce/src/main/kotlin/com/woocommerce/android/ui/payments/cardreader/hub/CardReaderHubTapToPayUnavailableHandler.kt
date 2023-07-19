package com.woocommerce.android.ui.payments.cardreader.hub

import androidx.annotation.StringRes
import com.woocommerce.android.R
import com.woocommerce.android.ui.payments.taptopay.TapToPayAvailabilityStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import javax.inject.Inject

class CardReaderHubTapToPayUnavailableHandler @Inject constructor() {
    fun handleTTPUnavailable(
        status: TapToPayAvailabilityStatus.Result.NotAvailable,
        triggerEvent: (MultiLiveEvent.Event) -> Unit,
        positiveButtonClick: (ActionType) -> Unit
    ) {
        when (status) {
            TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable -> {
                showDialog(
                    R.string.card_reader_tap_to_pay_not_available_error_nfc,
                    R.string.card_reader_upsell_card_reader_banner_cta,
                    triggerEvent
                ) { positiveButtonClick(ActionType.PURCHASE_READER) }
            }

            TapToPayAvailabilityStatus.Result.NotAvailable.SystemVersionNotSupported -> {
                showDialog(
                    R.string.card_reader_tap_to_pay_not_available_error_android_version,
                    R.string.card_reader_upsell_card_reader_banner_cta,
                    triggerEvent
                ) { positiveButtonClick(ActionType.PURCHASE_READER) }
            }

            TapToPayAvailabilityStatus.Result.NotAvailable.GooglePlayServicesNotAvailable -> {
                showDialog(
                    R.string.card_reader_tap_to_pay_not_available_error_gms,
                    R.string.card_reader_upsell_card_reader_banner_cta,
                    triggerEvent
                ) { positiveButtonClick(ActionType.PURCHASE_READER) }
            }

            TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported -> {
                showDialog(
                    R.string.card_reader_tap_to_pay_not_available_error_country,
                    R.string.card_reader_tap_to_pay_not_available_error_check_requirements_button,
                    triggerEvent
                ) { positiveButtonClick(ActionType.TAP_TO_PAY_REQUIREMENTS) }
            }
        }
    }

    private fun showDialog(
        @StringRes messageId: Int,
        @StringRes positiveButtonId: Int,
        triggerEvent: (MultiLiveEvent.Event) -> Unit,
        positiveButtonClick: () -> Unit,
    ) {
        triggerEvent(
            MultiLiveEvent.Event.ShowDialog(
                titleId = R.string.card_reader_tap_to_pay_not_available_error_title,
                messageId = messageId,
                positiveButtonId = positiveButtonId,
                negativeButtonId = R.string.close,
                positiveBtnAction = { _, _ -> positiveButtonClick() }
            )
        )
    }

    enum class ActionType {
        PURCHASE_READER, TAP_TO_PAY_REQUIREMENTS,
    }
}
