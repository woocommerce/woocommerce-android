package com.woocommerce.android.util

import android.os.Build
import android.telephony.PhoneNumberUtils
import com.woocommerce.android.util.WooLog.T
import java.util.Locale

object PhoneUtils {
    /**
     * Formats a phone number based on the users locale.
     */
    fun formatPhone(number: String): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                PhoneNumberUtils.formatNumber(number, Locale.getDefault().country)
            } else {
                @Suppress("DEPRECATION")
                PhoneNumberUtils.formatNumber(number)
            }
        } catch (e: Exception) {
            WooLog.d(T.UTILS, "Unable to format phone number: $number")
            number
        }
    }
}
