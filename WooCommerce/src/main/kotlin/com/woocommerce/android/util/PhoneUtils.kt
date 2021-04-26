package com.woocommerce.android.util

import android.telephony.PhoneNumberUtils
import com.woocommerce.android.util.WooLog.T
import java.util.Locale

object PhoneUtils {
    /**
     * Formats a phone number based on the users locale.
     */
    fun formatPhone(number: String): String {
        return try {
            PhoneNumberUtils.formatNumber(number, Locale.getDefault().country)
        } catch (e: Exception) {
            WooLog.e(T.UTILS, "Unable to format phone number: $number", e)
            number
        }
    }
}
