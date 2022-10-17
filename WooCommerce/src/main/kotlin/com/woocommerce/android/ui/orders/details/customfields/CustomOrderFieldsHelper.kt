package com.woocommerce.android.ui.orders.details.customfields

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.URLUtil
import com.woocommerce.android.R
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.StringUtils
import org.wordpress.android.util.ToastUtils

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
            CustomOrderFieldType.PHONE -> dialPhone(context, value)
            CustomOrderFieldType.TEXT -> {
                // no action
            }
        }
    }

    private fun showUrl(context: Context, url: String) {
        ChromeCustomTabUtils.launchUrl(context, url)
    }

    @Suppress("SwallowedException")
    private fun sendEmail(context: Context, emailAddr: String) {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:$emailAddr")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_email_app)
        }
    }

    @Suppress("SwallowedException")
    private fun dialPhone(context: Context, phone: String) {
        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:$phone")
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            ToastUtils.showToast(context, R.string.error_no_phone_app)
        }
    }
}
