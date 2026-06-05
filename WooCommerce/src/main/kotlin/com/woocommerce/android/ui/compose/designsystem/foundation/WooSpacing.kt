package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
@Suppress("LongParameterList")
data class WooSpacing(
    val space0: Dp,
    val space1: Dp,
    val space2: Dp,
    val space3: Dp,
    val space4: Dp,
    val space5: Dp,
    val space6: Dp,
    val space7: Dp,
    val space8: Dp,
    val space9: Dp,
    val space10: Dp,
    val space11: Dp,
    val space12: Dp,
)

internal val DefaultWooSpacing = WooSpacing(
    space0 = 0.dp,
    space1 = 2.dp,
    space2 = 4.dp,
    space3 = 8.dp,
    space4 = 12.dp,
    space5 = 16.dp,
    space6 = 20.dp,
    space7 = 24.dp,
    space8 = 32.dp,
    space9 = 40.dp,
    space10 = 48.dp,
    space11 = 56.dp,
    space12 = 64.dp,
)
