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

    /**
     * The device locale as a BCP 47 tag, e.g. `en-GB`. Unlike the app language, this is unaffected by a per-app
     * language override.
     */
    val localeTag: String?
        get() = ConfigurationCompat.getLocales(Resources.getSystem().configuration)[0]?.toLanguageTag()
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
    val localeTag: String?
        get() = DeviceInfo.localeTag
    val screenWidthDp: Int
        get() = Resources.getSystem().configuration.screenWidthDp
    val screenHeightDp: Int
        get() = Resources.getSystem().configuration.screenHeightDp
}
