package com.woocommerce.android.ui.payments.banner

import com.woocommerce.android.model.UiString

data class BannerState(
    val shouldDisplayBanner: Boolean,
    val onPrimaryActionClicked: () -> Unit,
    val onDismissClicked: () -> Unit,
    val title: UiString,
    val description: UiString,
    val primaryActionLabel: UiString,
    val chipLabel: UiString
)
