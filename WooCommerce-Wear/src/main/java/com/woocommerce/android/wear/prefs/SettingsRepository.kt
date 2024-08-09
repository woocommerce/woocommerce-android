package com.woocommerce.android.wear.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType
import com.woocommerce.commons.DataParameters.APP_SETTINGS
import com.woocommerce.commons.WearAppSettings
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class SettingsRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.SETTINGS) private val settingsDataStore: DataStore<Preferences>
) {
    private val gson by lazy { Gson() }

    suspend fun receiveAppSettingsDataFromPhone(data: DataMap) {
        val settings = data.getString(APP_SETTINGS.value, "")
            .takeIf { it.isNotEmpty() }
            ?.let { gson.fromJson(it, WearAppSettings::class.java) }
            ?: return

        storeSettingsKey(
            key = SettingsKey.CrashReportEnabled,
            value = settings.crashReportEnabled
        )
    }

    fun <T> fetchSettingsValue(key: SettingsKey<T>): Flow<T> =
        settingsDataStore.data.mapNotNull { it[key.prefsKey] }

    private suspend fun <T> storeSettingsKey(key: SettingsKey<T>, value: T) {
        settingsDataStore.edit { it[key.prefsKey] = value }
    }
}
