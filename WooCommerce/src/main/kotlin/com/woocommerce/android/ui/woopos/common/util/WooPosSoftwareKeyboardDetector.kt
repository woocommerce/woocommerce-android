package com.woocommerce.android.ui.woopos.common.util

import android.content.Context
import android.content.res.Configuration
import android.graphics.Rect
import android.view.View
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

data class KeyboardStatus(
    val isKeyboardVisible: Boolean,
    val hasHardwareKeyboard: Boolean,
    val isFloatingKeyboardEnabled: Boolean
)

@Composable
fun rememberKeyboardStatus(): KeyboardStatus {
    val view = LocalView.current
    val context = LocalContext.current
    var keyboardStatus by remember { mutableStateOf(getInitialKeyboardStatus(context)) }

    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            keyboardStatus = getCurrentKeyboardStatus(context, view)
        }
        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }

    return keyboardStatus
}


private fun getInitialKeyboardStatus(context: Context): KeyboardStatus {
    return KeyboardStatus(
        isKeyboardVisible = false,
        hasHardwareKeyboard = hasHardwareKeyboard(context),
        isFloatingKeyboardEnabled = isFloatingKeyboardEnabled(context)
    )
}

private fun getCurrentKeyboardStatus(context: Context, view: View): KeyboardStatus {
    return KeyboardStatus(
        isKeyboardVisible = isKeyboardVisible(view),
        hasHardwareKeyboard = hasHardwareKeyboard(context),
        isFloatingKeyboardEnabled = isFloatingKeyboardEnabled(context)
    )
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

private fun isFloatingKeyboardEnabled(context: Context): Boolean {
    val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
    return try {
        val enabledInputMethods = imm.enabledInputMethodList
        enabledInputMethods.isNotEmpty()
    } catch (e: SecurityException) {
        true
    }
}

private const val KEYBOARD_DETECTION_THRESHOLD = 0.15
