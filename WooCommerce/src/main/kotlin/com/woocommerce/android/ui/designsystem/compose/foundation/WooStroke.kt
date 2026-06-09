package com.woocommerce.android.ui.designsystem.compose.foundation

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
@Suppress("LongParameterList")
internal data class WooStroke(
    val none: Dp,
    val extraThin: Dp,
    val thin: Dp,
    val regular: Dp,
    val medium: Dp,
    val mediumIncreased: Dp,
    val thick: Dp,
    val extraThick: Dp,
)

internal val DefaultWooStroke = WooStroke(
    none = 0.dp,
    extraThin = 0.5.dp,
    thin = 0.75.dp,
    regular = 1.dp,
    medium = 1.5.dp,
    mediumIncreased = 2.dp,
    thick = 3.dp,
    extraThick = 4.dp,
)
