package com.woocommerce.android.ui.payments.banner

import com.woocommerce.android.model.UiString

sealed class BannerState {
    data class DisplayBannerState(
        val onPrimaryActionClicked: () -> Unit,
        val onDismissClicked: () -> Unit,
        val title: UiString,
        val description: UiString,
        val primaryActionLabel: UiString,
        val chipLabel: UiString
    ) : BannerState()

    object HideBannerState : BannerState()
}
