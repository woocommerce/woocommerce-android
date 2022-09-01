package com.woocommerce.android.ui.payments.banner

data class BannerState(
    val shouldDisplayBanner: Boolean,
    val onPrimaryActionClicked: () -> Unit,
    val onDismissClicked: () -> Unit,
    val title: Int,
    val description: Int,
    val primaryActionLabel: Int,
    val chipLabel: Int
)
