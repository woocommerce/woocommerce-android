package com.woocommerce.android.ui.orders.wooshippinglabels.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.StoreOptionsModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooShippingConfigurationDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SHIPPING_LABEL_CONFIGURATION) private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
    private val selectedSite: SelectedSite
) {
    private fun getStoreOptionsKey() = "${selectedSite.getOrNull()?.siteId ?: ""}StoreOptions"

    fun observeStoreOptions(): Flow<StoreOptionsModel?> {
        return dataStore.data.map { prefs ->
            val storeOptions = prefs[stringPreferencesKey(getStoreOptionsKey())]
            runCatching {
                gson.fromJson(storeOptions, StoreOptionsModel::class.java)
            }.getOrNull()
        }
    }

    suspend fun saveStoreOptions(storeOptions: StoreOptionsModel) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(getStoreOptionsKey())] = gson.toJson(storeOptions)
        }
    }

    suspend fun clearStoreOptions() = dataStore.edit { it.remove(stringPreferencesKey(getStoreOptionsKey())) }
}
