package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.ui.unit.Dp

sealed interface WooPosPinPadStyle {
    data object Standard : WooPosPinPadStyle

    data class LockScreen(val keySize: Dp, val showsDisabledKeys: Boolean) : WooPosPinPadStyle
}
