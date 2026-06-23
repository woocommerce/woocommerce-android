package com.woocommerce.android.ui.compose.designsystem.foundation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Immutable
@Suppress("LongParameterList")
data class WooRadius(
    val none: Dp,
    val extraSmall: Dp,
    val small: Dp,
    val medium: Dp,
    val large: Dp,
    val extraLarge: Dp,
    val full: Dp,
)

internal val DefaultWooRadius = WooRadius(
    none = 0.dp,
    extraSmall = 2.dp,
    small = 4.dp,
    medium = 8.dp,
    large = 12.dp,
    extraLarge = 16.dp,
    full = 999.dp,
)

internal fun WooRadius.toMaterialShapes(): Shapes = Shapes(
    extraSmall = RoundedCornerShape(extraSmall),
    small = RoundedCornerShape(small),
    medium = RoundedCornerShape(medium),
    large = RoundedCornerShape(large),
    extraLarge = RoundedCornerShape(extraLarge),
)
