package com.woocommerce.android.util

import android.content.Context
import androidx.annotation.StringRes
import com.woocommerce.android.R

private const val WHATSAPP_PACKAGE_NAME = "com.whatsapp"
private const val TELEGRAM_PACKAGE_NAME = "org.telegram.messenger"

fun Context.getAvailablePhoneContactOptions(): List<PhoneContactOption> {
    return buildList {
        add(PhoneContactOption.CALL)
        add(PhoneContactOption.SMS)
        if (ActivityUtils.isAppInstalled(this@getAvailablePhoneContactOptions, WHATSAPP_PACKAGE_NAME)) {
            add(PhoneContactOption.WHATSAPP)
        }
        if (ActivityUtils.isAppInstalled(this@getAvailablePhoneContactOptions, TELEGRAM_PACKAGE_NAME)) {
            add(PhoneContactOption.TELEGRAM)
        }
    }
}

enum class PhoneContactOption {
    CALL, SMS, WHATSAPP, TELEGRAM
}

val PhoneContactOption.stringRes: Int
    @StringRes get() = when (this) {
        PhoneContactOption.CALL -> R.string.orderdetail_call_customer
        PhoneContactOption.SMS -> R.string.orderdetail_message_customer
        PhoneContactOption.WHATSAPP -> R.string.orderdetail_message_customer_using_whatsapp
        PhoneContactOption.TELEGRAM -> R.string.orderdetail_message_customer_using_telegram
    }
