package com.woocommerce.android.util

import android.content.Context
import android.os.Build
import android.telephony.PhoneNumberUtils
import java.util.Locale

object PhoneUtils {
    /**
     * Formats a phone number based on the users locale.
     */
    fun formatPhone(context: Context, number: String): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            PhoneNumberUtils.formatNumber(number, Locale.getDefault().country)
        } else {
            @Suppress("DEPRECATION")
            PhoneNumberUtils.formatNumber(number)
        }
    }
}
