package com.woocommerce.android.ui.orders.wooshippinglabels.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ConfigDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingNetworkingMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooShippingConfigDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SHIPPING_LABELS_DATA) private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
    private val selectedSite: SelectedSite,
    private val mapper: WooShippingNetworkingMapper,
) {
    private fun getConfigKey(orderId: Long) = "${selectedSite.get().id}:${orderId}Config"

    fun observeConfig(orderId: Long): Flow<ConfigDTO?> = dataStore.data.map { prefs ->
        val config = prefs[stringPreferencesKey(getConfigKey(orderId))]
        runCatching { gson.fromJson(config, ConfigDTO::class.java) }.getOrNull()
    }.distinctUntilChanged()

    fun getShippingLabel(orderId: Long, labelId: Long) = observeConfig(orderId).map { config ->
        config?.shippingLabelData?.currentOrderLabels
            ?.find { it.labelId == labelId }
            ?.let { mapper(it) }
    }

    suspend fun saveConfig(orderId: Long, config: ConfigDTO) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(getConfigKey(orderId))] = gson.toJson(config)
        }
    }
}
