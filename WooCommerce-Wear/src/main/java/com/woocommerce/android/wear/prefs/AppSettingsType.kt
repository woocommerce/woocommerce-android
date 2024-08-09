package com.woocommerce.android.wear.prefs

import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey

sealed class SettingsType<T>(
    open val prefsKey: Preferences.Key<T>
) {
    data object CrashReportEnabled : SettingsType<Boolean>(
        prefsKey = booleanPreferencesKey("crash_report_enabled")
    )
}
