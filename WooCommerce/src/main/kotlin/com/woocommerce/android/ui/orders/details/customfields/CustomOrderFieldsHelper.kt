package com.woocommerce.android.ui.orders.details.customfields

import android.content.Context
import android.webkit.URLUtil
import com.woocommerce.android.util.ActivityUtils
import com.woocommerce.android.util.ActivityUtils.sendEmail
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.StringUtils

object CustomOrderFieldsHelper {
    interface CustomOrderFieldClickListener {
        fun onCustomOrderFieldClicked(value: String)
    }

    enum class CustomOrderFieldType {
        TEXT,
        URL,
        EMAIL,
        PHONE;

        companion object {
            fun fromMetadataValue(value: String): CustomOrderFieldType {
                return when {
                    URLUtil.isValidUrl(value) -> URL
                    StringUtils.isValidEmail(value) -> EMAIL
                    value.startsWith("tel://") -> PHONE
                    else -> TEXT
                }
            }
        }
    }

    fun handleMetadataValue(context: Context, value: String) {
        when (CustomOrderFieldType.fromMetadataValue(value)) {
            CustomOrderFieldType.EMAIL -> sendEmail(context, value)
            CustomOrderFieldType.URL -> showUrl(context, value)
            CustomOrderFieldType.PHONE -> ActivityUtils.dialPhoneNumber(context, value)
            CustomOrderFieldType.TEXT -> {
                // no action
            }
        }
    }

    private fun showUrl(context: Context, url: String) {
        ChromeCustomTabUtils.launchUrl(context, url)
    }
}
