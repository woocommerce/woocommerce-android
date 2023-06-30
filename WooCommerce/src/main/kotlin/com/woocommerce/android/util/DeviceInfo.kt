package com.woocommerce.android.util

import android.content.res.Resources
import android.os.Build
import androidx.core.os.ConfigurationCompat
import javax.inject.Inject
import javax.inject.Singleton

object DeviceInfo {
    val OS: String
        get() = Build.VERSION.RELEASE
    val OSCode: Int
        get() = Build.VERSION.SDK_INT
    val name: String
        get() {
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            return if (model.startsWith(manufacturer)) {
                model
            } else {
                "$manufacturer $model"
            }
        }
    val locale: String?
        get() {
            val locale = ConfigurationCompat.getLocales(Resources.getSystem().configuration)
            return locale[0]?.displayLanguage
        }
}

@Singleton
class DeviceInfoWrapper @Inject constructor() {
    val osName: String
        get() = DeviceInfo.OS
    val osVersionCode: Int
        get() = DeviceInfo.OSCode
    val name: String
        get() = DeviceInfo.name
    val locale: String?
        get() = DeviceInfo.locale
}
