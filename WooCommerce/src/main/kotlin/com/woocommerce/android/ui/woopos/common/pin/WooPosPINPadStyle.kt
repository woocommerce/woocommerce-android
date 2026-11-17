package com.woocommerce.android.ui.woopos.common.pin

import androidx.compose.ui.unit.Dp

sealed interface WooPosPINPadStyle {
    data object Standard : WooPosPINPadStyle

    data class LockScreen(val keySize: Dp, val keySpacing: Dp, val showsDisabledKeys: Boolean) : WooPosPINPadStyle
}
