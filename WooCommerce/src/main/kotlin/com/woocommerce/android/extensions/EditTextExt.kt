package com.woocommerce.android.extensions

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.view.View
import android.view.autofill.AutofillManager
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

fun EditText.requestPasswordAutoFillWithDelay(delayMs: Long = DEFAULT_AUTOFILL_DELAY) {
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
        setAutofillHints(View.AUTOFILL_HINT_PASSWORD)
        importantForAutofill = View.IMPORTANT_FOR_AUTOFILL_YES
        val af = context.getSystemService(AutofillManager::class.java)
        postDelayed(
            {
                af.requestAutofill(this)
            }, delayMs
        )
    }
}

private const val DEFAULT_KEYBOARD_DELAY = 500L
private const val DEFAULT_AUTOFILL_DELAY = 500L
