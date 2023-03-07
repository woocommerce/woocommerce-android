package com.woocommerce.android.extensions

import android.widget.EditText
import org.wordpress.android.util.ActivityUtils

/**
 * Useful for showing the soft keyboard when the user enters a fragment
 */
fun EditText.showKeyboardWithDelay(delayMs: Long = DEFAULT_KEYBOARD_DELAY) {
    requestFocus()
    postDelayed(
        {
            ActivityUtils.showKeyboard(this)
        }, delayMs
    )
}

private const val DEFAULT_KEYBOARD_DELAY = 500L
