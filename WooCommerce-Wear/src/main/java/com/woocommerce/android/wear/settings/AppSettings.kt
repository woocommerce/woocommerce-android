package com.woocommerce.android.wear.settings

import android.content.SharedPreferences
import com.woocommerce.commons.prefs.PreferenceUtils

sealed class AppSettings<T> {
    abstract var value: T
    abstract val key: String

    data class CrashReportEnabledSettings(
        private val preferences: SharedPreferences
    ) : AppSettings<Boolean>() {
        override val key = this::class.simpleName.orEmpty()

        override var value: Boolean
            get() = PreferenceUtils.getBoolean(preferences, key, true)
            set(value) = PreferenceUtils.setBoolean(preferences, key, value)
    }
}
