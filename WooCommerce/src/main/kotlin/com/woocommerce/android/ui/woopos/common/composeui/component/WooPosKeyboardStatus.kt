package com.woocommerce.android.ui.woopos.common.composeui.component

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

enum class WooPosKeyboardStatus {
    SoftwareKeyboardVisible,
    HardwareKeyboardConnected,
    BothKeyboardsVisible,
    NoKeyboardVisible
}

@Composable
fun rememberKeyboardStatus(): WooPosKeyboardStatus {
    val view = LocalView.current
    val context = LocalContext.current
    var keyboardStatus by remember { mutableStateOf(getInitialKeyboardStatus(context)) }
    var lastUpdateTime by remember { mutableLongStateOf(0L) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastUpdateTime >= DEBOUNCE_DELAY_MS) {
                keyboardStatus = getCurrentKeyboardStatus(context, view)
                lastUpdateTime = currentTime
            }
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return keyboardStatus
}

private fun getInitialKeyboardStatus(context: Context): WooPosKeyboardStatus {
    val hasHardware = hasHardwareKeyboard(context)
    return if (hasHardware) {
        WooPosKeyboardStatus.HardwareKeyboardConnected
    } else {
        WooPosKeyboardStatus.NoKeyboardVisible
    }
}

private fun getCurrentKeyboardStatus(context: Context, view: View): WooPosKeyboardStatus {
    val isSoftwareVisible = isKeyboardVisible(view)
    val hasHardware = hasHardwareKeyboard(context)
    return when {
        isSoftwareVisible && hasHardware -> WooPosKeyboardStatus.BothKeyboardsVisible
        isSoftwareVisible && !hasHardware -> WooPosKeyboardStatus.SoftwareKeyboardVisible
        !isSoftwareVisible && hasHardware -> WooPosKeyboardStatus.HardwareKeyboardConnected
        else -> WooPosKeyboardStatus.NoKeyboardVisible
    }
}

private fun isKeyboardVisible(view: View): Boolean {
    val rect = Rect()
    view.getWindowVisibleDisplayFrame(rect)
    val screenHeight = view.rootView.height
    val keypadHeight = screenHeight - rect.bottom

    return keypadHeight > screenHeight * KEYBOARD_DETECTION_THRESHOLD
}

private fun hasHardwareKeyboard(context: Context): Boolean {
    val configuration = context.resources.configuration
    return configuration.keyboard != Configuration.KEYBOARD_NOKEYS &&
        configuration.hardKeyboardHidden == Configuration.HARDKEYBOARDHIDDEN_NO
}

private const val KEYBOARD_DETECTION_THRESHOLD = 0.15
private const val DEBOUNCE_DELAY_MS = 1000L
