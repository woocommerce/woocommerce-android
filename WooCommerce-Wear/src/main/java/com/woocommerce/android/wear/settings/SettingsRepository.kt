package com.woocommerce.android.wear.settings

import android.content.Context
import androidx.preference.PreferenceManager
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.settings.AppSettings.CrashReportEnabledSettings
import com.woocommerce.commons.DataParameters.APP_SETTINGS
import com.woocommerce.commons.WearAppSettings
import javax.inject.Inject

class SettingsRepository @Inject constructor(
    private val appContext: Context
) {
    private val gson by lazy { Gson() }

    private val preferences
        get() = PreferenceManager.getDefaultSharedPreferences(appContext)

    val crashReportEnabled: CrashReportEnabledSettings
        get() = CrashReportEnabledSettings(preferences)

    fun receiveAppSettingsDataFromPhone(data: DataMap) {
        val settings = data.getString(APP_SETTINGS.value, "")
            .takeIf { it.isNotEmpty() }
            ?.let { gson.fromJson(it, WearAppSettings::class.java) }
            ?: return

        crashReportEnabled.value = settings.crashReportEnabled
    }
}
