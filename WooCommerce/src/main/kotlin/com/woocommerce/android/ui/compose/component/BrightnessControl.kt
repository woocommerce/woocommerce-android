package com.woocommerce.android.ui.compose.component

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

@Composable
fun BrightnessControl(temporarilyIncreaseToMax: Boolean = true) {
    val context = LocalContext.current
    var originalBrightness by remember { mutableStateOf<Float?>(null) }

    LaunchedEffect(temporarilyIncreaseToMax) {
        val activity = context as? Activity ?: return@LaunchedEffect
        val window = activity.window

        if (temporarilyIncreaseToMax) {
            if (originalBrightness == null) {
                originalBrightness = window.attributes.screenBrightness
            }
            window.attributes = window.attributes.apply {
                screenBrightness = 1.0f
            }
        } else {
            originalBrightness?.let {
                window.attributes = window.attributes.apply {
                    screenBrightness = it
                }
                originalBrightness = null
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (temporarilyIncreaseToMax && originalBrightness != null) {
                val activity = context as? Activity ?: return@onDispose
                activity.window.attributes = activity.window.attributes.apply {
                    screenBrightness = originalBrightness ?: -1.0f
                }
            }
        }
    }
}
