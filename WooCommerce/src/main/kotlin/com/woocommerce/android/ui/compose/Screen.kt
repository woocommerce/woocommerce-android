package com.woocommerce.android.ui.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.woocommerce.android.ui.compose.Screen.ScreenType.Large
import com.woocommerce.android.ui.compose.Screen.ScreenType.Medium
import com.woocommerce.android.ui.compose.Screen.ScreenType.Small

@Composable
fun rememberScreen(): Screen {
    val (screenWidth, screenHeight) = LocalConfiguration.current.let {
        Pair(it.screenWidthDp, it.screenHeightDp)
    }

    return remember(screenWidth, screenHeight) {
        Screen(
            width = screenWidth.dp,
            height = screenHeight.dp,
            type = when {
                screenWidth <= Small.width -> Small
                screenWidth <= Medium.width -> Medium
                else -> Large
            }
        )
    }
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

enum class DeviceType { Phone, Tablet, }
