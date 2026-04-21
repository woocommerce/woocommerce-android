package com.woocommerce.android.ui.woopos.util

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object WooPosScreenSizeUtils {
    const val MIN_TABLET_SHORT_SIZE_DP = 674
    const val MIN_TABLET_LONG_SIZE_DP = 800

    fun isTabletSize(shortSize: Dp, longSize: Dp): Boolean =
        shortSize >= MIN_TABLET_SHORT_SIZE_DP.dp && longSize >= MIN_TABLET_LONG_SIZE_DP.dp
}
