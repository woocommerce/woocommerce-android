package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.runtime.Composable
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

enum class WooPosSpacing(val value: Dp) {
    None(0.dp),
    XSmall(4.dp),
    Small(8.dp),
    Medium(16.dp),
    Large(24.dp),
    XLarge(32.dp)
}

enum class WooPosElevation(val value: Dp) {
    None(0.dp),
    Medium(8.dp),
    Large(24.dp)
}

@Composable
fun Dp.toAdaptivePadding(): Dp {
    val longestSide = LocalContext.current.getLongestScreenSideDp()
    return when {
        longestSide < 880.dp -> {
            val calculatedMargin = this * 0.5f
            calculatedMargin.makeDividableByFour()
        }

        longestSide < 1200.dp -> {
            val calculatedMargin = this * 0.75f
            calculatedMargin.makeDividableByFour()
        }

        else -> this
    }
}

@Composable
private fun Dp.makeDividableByFour(): Dp {
    val remainder = this.value % 4
    return if (remainder == 0f) {
        this
    } else {
        this + (4 - remainder).dp
    }
}
