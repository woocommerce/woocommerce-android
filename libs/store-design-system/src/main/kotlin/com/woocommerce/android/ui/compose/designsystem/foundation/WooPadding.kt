package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
@Suppress("LongParameterList")
data class WooPadding(
    val padding0: Dp,
    val padding1: Dp,
    val padding2: Dp,
    val padding3: Dp,
    val padding4: Dp,
    val padding5: Dp,
    val padding6: Dp,
    val padding7: Dp,
    val padding8: Dp,
    val padding9: Dp,
    val padding10: Dp,
    val padding11: Dp,
    val padding12: Dp,
)

internal val DefaultWooPadding = WooPadding(
    padding0 = 0.dp,
    padding1 = 2.dp,
    padding2 = 4.dp,
    padding3 = 8.dp,
    padding4 = 12.dp,
    padding5 = 16.dp,
    padding6 = 20.dp,
    padding7 = 24.dp,
    padding8 = 32.dp,
    padding9 = 40.dp,
    padding10 = 48.dp,
    padding11 = 56.dp,
    padding12 = 64.dp,
)

internal val LocalWooPadding = staticCompositionLocalOf<WooPadding> {
    error("WooTheme.padding is not available. Wrap content in WooDesignSystemTheme.")
}
