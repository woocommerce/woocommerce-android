package com.woocommerce.android.wear.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

class SettingsRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.SETTINGS) private val settingsDataStore: DataStore<Preferences>
) {
    suspend fun <T> storeSettingsKey(key: SettingsKey<T>, value: T) {
        settingsDataStore.edit { it[key.prefsKey] = value }
    }

    fun <T> fetchSettingsValue(key: SettingsKey<T>): Flow<T> =
        settingsDataStore.data.mapNotNull { it[key.prefsKey] }
}
