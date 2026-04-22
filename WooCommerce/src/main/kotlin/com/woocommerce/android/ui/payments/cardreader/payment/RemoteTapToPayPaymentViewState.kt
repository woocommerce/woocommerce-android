package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString.UiStringRes

data class RemoteTapToPayStarting(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_starting_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_starting_subtitle),
    illustration = null,
    primaryActionLabel = R.string.card_reader_mode_cancel,
)

data class RemoteTapToPayReadyToPair(
    val deviceName: String,
    val fingerprintSuffix: String,
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_ready_to_pair_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_ready_to_pair_subtitle),
    illustration = R.drawable.img_card_reader_tpp_connecting,
    primaryActionLabel = R.string.card_reader_mode_cancel,
)

data class RemoteTapToPayWaitingForPayment(
    val tabletName: String?,
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_waiting_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_waiting_subtitle),
    illustration = R.drawable.img_card_reader_tpp_collecting_payment,
    primaryActionLabel = R.string.card_reader_mode_cancel,
)
