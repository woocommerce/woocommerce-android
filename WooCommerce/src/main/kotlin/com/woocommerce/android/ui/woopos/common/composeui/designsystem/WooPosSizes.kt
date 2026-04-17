package com.woocommerce.android.ui.woopos.common.composeui.designsystem

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed

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
        @Composable get() = when (currentWooPosBreakpoint()) {
            WooPosBreakpoint.Phone -> phoneValue
            WooPosBreakpoint.SmallTablet -> smallTabletValue
            WooPosBreakpoint.Tablet -> tabletValue
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
        @Composable get() = baseValue.toAdaptivePadding()
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
        @Composable get() = when (currentWooPosBreakpoint()) {
            WooPosBreakpoint.Phone -> phoneValue
            WooPosBreakpoint.SmallTablet -> smallTabletValue
            WooPosBreakpoint.Tablet -> tabletValue
        }
}

enum class WooPosIconSize(val value: Dp) {
    XSmall(16.dp),
    Small(24.dp),
    Medium(32.dp),
    Large(40.dp),
    XLarge(48.dp)
}

enum class WooPosComponentSize(private val baseValue: Dp) {
    XXSmall(40.dp),
    XSmall(56.dp),
    Small(80.dp),
    Medium(96.dp),
    Large(112.dp),
    XLarge(160.dp),
    XXLarge(256.dp);

    val value: Dp
        @Composable get() = baseValue.toAdaptiveComponentSize()
}

@Composable
fun Dp.toAdaptivePadding(): Dp = when (currentWooPosBreakpoint()) {
    WooPosBreakpoint.Phone -> (this * 0.5f).makeDividableByFour()
    WooPosBreakpoint.SmallTablet -> (this * 0.75f).makeDividableByFour()
    WooPosBreakpoint.Tablet -> this
}

@Composable
fun Dp.toAdaptiveComponentSize(): Dp = when (currentWooPosBreakpoint()) {
    WooPosBreakpoint.Phone -> (this * 0.75f).makeDividableByFour()
    WooPosBreakpoint.SmallTablet -> (this * 0.9f).makeDividableByFour()
    WooPosBreakpoint.Tablet -> this
}

@Composable
fun Dp.toAdaptiveIconSize(): Dp = when (currentWooPosBreakpoint()) {
    WooPosBreakpoint.Phone -> (this * 0.9f).makeDividableByFour()
    WooPosBreakpoint.SmallTablet -> (this * 0.95f).makeDividableByFour()
    WooPosBreakpoint.Tablet -> this
}

@Composable
fun Modifier.adaptiveContentWidth(): Modifier = when (currentWooPosBreakpoint()) {
    WooPosBreakpoint.Phone -> this.fillMaxWidth()
    WooPosBreakpoint.SmallTablet -> this.fillMaxWidth(fraction = 2f / 3f)
    WooPosBreakpoint.Tablet -> this.fillMaxWidth(fraction = 0.55f)
}

internal enum class WooPosBreakpoint { Phone, SmallTablet, Tablet }

@Composable
internal fun currentWooPosBreakpoint(): WooPosBreakpoint {
    val density = LocalDensity.current
    val containerSize = LocalWindowInfo.current.containerSize
    val shortSize = with(density) { minOf(containerSize.width, containerSize.height).toDp() }
    val longSize = with(density) { maxOf(containerSize.width, containerSize.height).toDp() }
    return when {
        !WooPosIsScreenSizeAllowed.isTabletSize(shortSize, longSize) -> WooPosBreakpoint.Phone
        shortSize < BIG_TABLET_SHORT_SIZE_DP.dp -> WooPosBreakpoint.SmallTablet
        else -> WooPosBreakpoint.Tablet
    }
}

private const val BIG_TABLET_SHORT_SIZE_DP = 800

@Suppress("MagicNumber")
private fun Dp.makeDividableByFour(): Dp {
    val remainder = this.value % 4
    return if (remainder == 0f) this else this + (4 - remainder).dp
}
