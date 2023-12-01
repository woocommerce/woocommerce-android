package com.woocommerce.android.ui.compose

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.Screen.ScreenType.Desktop
import com.woocommerce.android.ui.compose.Screen.ScreenType.Mobile
import com.woocommerce.android.ui.compose.Screen.ScreenType.Tablet

@Composable
fun rememberScreen(): Screen {
    val configuration = LocalConfiguration.current
    return Screen(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp,
        type = if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            when {
                configuration.screenWidthDp <= Mobile.width -> Mobile
                configuration.screenWidthDp <= Tablet.width -> Tablet
                else -> Desktop
            }
        } else {
            when {
                configuration.screenWidthDp <= Mobile.height -> Mobile
                configuration.screenWidthDp <= Tablet.height -> Tablet
                else -> Desktop
            }
        }
    )
}

data class Screen(
    val width: Dp,
    val height: Dp,
    val type: ScreenType,
) {
    sealed class ScreenType(val width: Int, val height: Int) {
        object Mobile : ScreenType(width = 420, height = 960)
        object Tablet : ScreenType(width = 840, height = 1280)
        object Desktop : ScreenType(width = 1280, height = 960)
    }
}
