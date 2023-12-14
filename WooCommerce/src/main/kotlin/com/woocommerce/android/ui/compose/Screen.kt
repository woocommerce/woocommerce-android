package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.Screen.ScreenType.Large
import com.woocommerce.android.ui.compose.Screen.ScreenType.Medium
import com.woocommerce.android.ui.compose.Screen.ScreenType.Small

@Composable
fun rememberScreen(): Screen {
    val configuration = LocalConfiguration.current
    return Screen(
        width = configuration.screenWidthDp.dp,
        height = configuration.screenHeightDp.dp,
        type = when {
            configuration.screenWidthDp <= Small.width -> Small
            configuration.screenWidthDp <= Medium.width -> Medium
            else -> Large
        }
    )
}

data class Screen(
    val width: Dp,
    val height: Dp,
    val type: ScreenType,
) {
    sealed class ScreenType(val width: Int) {
        object Small : ScreenType(width = 420)
        object Medium : ScreenType(width = 840)
        object Large : ScreenType(width = 1280)
    }
}
