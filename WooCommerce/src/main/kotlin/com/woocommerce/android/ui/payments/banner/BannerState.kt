package com.woocommerce.android.ui.payments.banner

import com.woocommerce.android.model.UiString

sealed class BannerState {
    data class DisplayBannerState(
        val onPrimaryActionClicked: () -> Unit,
        val onDismissClicked: () -> Unit,
        val title: UiString,
        val description: UiString,
        val primaryActionLabel: UiString,
        val primaryIcon: LocalOrRemoteIcon,
        val secondaryIcon: LabelOrRemoteIcon,
    ) : BannerState()

    object HideBannerState : BannerState()

    sealed class LocalOrRemoteIcon {
        data class Local(val drawableId: Int) : LocalOrRemoteIcon()
        data class RemoteIcon(val url: String) : LocalOrRemoteIcon()
    }

    sealed class LabelOrRemoteIcon {
        data class Label(val label: UiString) : LabelOrRemoteIcon()
        data class RemoteIcon(val url: String) : LabelOrRemoteIcon()
    }
}
