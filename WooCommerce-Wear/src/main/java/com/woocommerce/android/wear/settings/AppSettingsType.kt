package com.woocommerce.android.wear.settings

import android.content.SharedPreferences
import com.woocommerce.commons.prefs.PreferenceUtils

sealed class AppSettings<T>(
    private val preferences: SharedPreferences
) {
    abstract var value: T
    abstract val key: String

    data class CrashReportEnabled(
        private val preferences: SharedPreferences
    ) : AppSettings<Boolean>(preferences) {
        override val key = this::class.simpleName.orEmpty()

        override var value: Boolean
            get() = PreferenceUtils.getBoolean(preferences, key, false)
            set(value) = PreferenceUtils.setBoolean(preferences, key, value)
    }
}
