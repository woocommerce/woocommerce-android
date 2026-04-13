package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.util.ext.getLongestScreenSideDp

enum class WooPosCornerRadius(val value: Dp) {
    None(0.dp),
    XSmall(2.dp),
    Small(4.dp),
    Medium(8.dp),
    Large(16.dp),
    XLarge(24.dp)
}

enum class WooPosSpacing(private val baseValue: Dp) {
    None(0.dp),
    XXSmall(2.dp),
    XSmall(4.dp),
    Small(8.dp),
    Medium(16.dp),
    Large(24.dp),
    XLarge(32.dp),
    XXLarge(40.dp),
    XXXLarge(48.dp),
    Huge(80.dp),
    Gigantic(104.dp);

    val value: Dp
        @Composable get() = baseValue.toAdaptivePadding()
}

enum class WooPosElevation(val value: Dp) {
    None(0.dp),
    Medium(8.dp),
    Large(24.dp)
}

enum class WooPosIconSize(val value: Dp) {
    XSmall(16.dp),
    Small(24.dp),
    Medium(32.dp),
    Large(40.dp),
    XLarge(48.dp)
}

@Composable
fun Dp.toAdaptivePadding(): Dp {
    val longestSide = LocalContext.current.getLongestScreenSideDp()
    return when {
        longestSide < 880.dp -> (this * 0.5f).makeDividableByFour()
        longestSide < 1200.dp -> (this * 0.75f).makeDividableByFour()
        else -> this
    }
}

@Composable
fun Dp.toAdaptiveComponentSize(): Dp {
    val longestSide = LocalContext.current.getLongestScreenSideDp()
    return when {
        longestSide < 880.dp -> (this * 0.75f).makeDividableByFour()
        longestSide < 1200.dp -> (this * 0.9f).makeDividableByFour()
        else -> this
    }
}

@Composable
fun Dp.toAdaptiveIconSize(): Dp {
    val longestSide = LocalContext.current.getLongestScreenSideDp()
    return when {
        longestSide < 880.dp -> (this * 0.9f).makeDividableByFour()
        longestSide < 1200.dp -> (this * 0.95f).makeDividableByFour()
        else -> this
    }
}

@Composable
fun Modifier.adaptiveContentWidth(): Modifier {
    val configuration = LocalConfiguration.current
    val longestSide = maxOf(configuration.screenWidthDp, configuration.screenHeightDp).dp
    return when {
        longestSide < 880.dp -> this.fillMaxWidth()
        longestSide < 1200.dp -> this.fillMaxWidth(fraction = 2f / 3f)
        else -> this.fillMaxWidth(fraction = 3f / 5f)
    }
}

@Suppress("MagicNumber")
private fun Dp.makeDividableByFour(): Dp {
    val remainder = this.value % 4
    return if (remainder == 0f) this else this + (4 - remainder).dp
}
