package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.util.ext.getLongestScreenSideDp

enum class WooPosCornerRadius(
    private val tabletValue: Dp,
    private val smallTabletValue: Dp,
    private val phoneValue: Dp,
) {
    None(0.dp, 0.dp, 0.dp),
    XSmall(4.dp, 4.dp, 4.dp),
    Small(4.dp, 4.dp, 4.dp),
    Medium(8.dp, 8.dp, 4.dp),
    Large(16.dp, 12.dp, 8.dp),
    XLarge(24.dp, 20.dp, 16.dp);

    val value: Dp
        @Composable get() {
            val longestSide = LocalContext.current.getLongestScreenSideDp()
            return when {
                longestSide < 880.dp -> phoneValue
                longestSide < 1200.dp -> smallTabletValue
                else -> tabletValue
            }
        }
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
        @Composable get() = baseValue.toAdaptiveSpacing()
}

enum class WooPosElevation(
    private val tabletValue: Dp,
    private val smallTabletValue: Dp,
    private val phoneValue: Dp,
) {
    None(0.dp, 0.dp, 0.dp),
    Medium(8.dp, 8.dp, 4.dp),
    Large(24.dp, 20.dp, 16.dp);

    val value: Dp
        @Composable get() {
            val longestSide = LocalContext.current.getLongestScreenSideDp()
            return when {
                longestSide < 880.dp -> phoneValue
                longestSide < 1200.dp -> smallTabletValue
                else -> tabletValue
            }
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
private fun Dp.toAdaptiveSpacing(): Dp {
    val longestSide = LocalContext.current.getLongestScreenSideDp()
    return when {
        longestSide < 880.dp -> (this * 0.625f).makeDividableByFour()
        longestSide < 1200.dp -> (this * 0.8f).makeDividableByFour()
        else -> this
    }
}

@Suppress("MagicNumber")
private fun Dp.makeDividableByFour(): Dp {
    val remainder = this.value % 4
    return if (remainder == 0f) {
        this
    } else {
        this + (4 - remainder).dp
    }
}
