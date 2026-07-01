package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
@Suppress("LongParameterList")
data class WooIconSize(
    val size14: Dp,
    val size16: Dp,
    val size18: Dp,
    val size20: Dp,
    val size24: Dp,
    val size32: Dp,
)

internal val DefaultWooIconSize = WooIconSize(
    size14 = 14.dp,
    size16 = 16.dp,
    size18 = 18.dp,
    size20 = 20.dp,
    size24 = 24.dp,
    size32 = 32.dp,
)

internal val LocalWooIconSize = staticCompositionLocalOf<WooIconSize> {
    error("WooTheme.iconSize is not available. Wrap content in WooDesignSystemTheme.")
}
