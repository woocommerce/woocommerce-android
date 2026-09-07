package com.woocommerce.android.ui.payments.cardreader.payment

import com.woocommerce.android.R
import com.woocommerce.android.model.UiString
import com.woocommerce.android.model.UiString.UiStringRes
import com.woocommerce.android.model.UiString.UiStringText

data class RemoteTapToPayStarting(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_starting_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_starting_subtitle),
    illustration = null,
    primaryActionLabel = R.string.card_reader_mode_cancel,
)

data class RemoteTapToPayLocationPermissionExplainer(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_location_permission_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_location_permission_subtitle),
    illustration = R.drawable.img_card_reader_tpp_connecting,
    primaryActionLabel = R.string.card_reader_mode_location_permission_continue,
)

data class RemoteTapToPayLocationPermissionDenied(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_location_permission_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_location_permission_denied_subtitle),
    illustration = R.drawable.img_card_reader_tpp_connecting,
    primaryActionLabel = R.string.card_reader_mode_location_permission_open_settings,
)

data class RemoteTapToPayLocalNetworkPermissionExplainer(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_local_network_permission_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_local_network_permission_subtitle),
    illustration = R.drawable.img_card_reader_tpp_connecting,
    primaryActionLabel = R.string.card_reader_mode_local_network_permission_continue,
)

data class RemoteTapToPayLocalNetworkPermissionDenied(
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_local_network_permission_header,
    paymentStateLabel = UiStringRes(R.string.card_reader_mode_local_network_permission_denied_subtitle),
    illustration = R.drawable.img_card_reader_tpp_connecting,
    primaryActionLabel = R.string.card_reader_mode_local_network_permission_open_settings,
)

data class RemoteTapToPayReadyToPair(
    val deviceName: String,
    val fingerprintSuffix: String,
    val siteUrl: String?,
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

data class RemoteTapToPayError(
    val message: String?,
    override val onPrimaryActionClicked: (() -> Unit),
) : ViewState(
    headerLabel = R.string.card_reader_mode_error_header,
    paymentStateLabel = errorPaymentStateLabel(message),
    illustration = R.drawable.img_products_error,
    primaryActionLabel = R.string.card_reader_mode_error_close,
)

private fun errorPaymentStateLabel(message: String?): UiString =
    if (message.isNullOrBlank()) {
        UiStringRes(R.string.card_reader_mode_error_subtitle)
    } else {
        UiStringText(message)
    }
