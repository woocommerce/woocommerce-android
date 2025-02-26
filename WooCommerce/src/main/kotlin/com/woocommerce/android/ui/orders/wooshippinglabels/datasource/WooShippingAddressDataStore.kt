package com.woocommerce.android.ui.orders.wooshippinglabels.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooShippingAddressDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SHIPPING_LABEL_ADDRESS) private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
    private val selectedSite: SelectedSite
) {
    private fun getOriginAddressesKey() = "${selectedSite.getOrNull()?.siteId ?: ""}OriginAddresses"

    fun observeOriginAddresses(): Flow<List<OriginShippingAddress>?> {
        val typeToken = object : TypeToken<List<OriginShippingAddress>>() {}.type
        return dataStore.data.map { prefs ->
            val storeOptions = prefs[stringPreferencesKey(getOriginAddressesKey())]
            runCatching {
                gson.fromJson<List<OriginShippingAddress>>(storeOptions, typeToken)
            }.getOrNull()
        }
    }

    suspend fun saveOriginAddresses(addresses: List<OriginShippingAddress>) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(getOriginAddressesKey())] = gson.toJson(addresses)
        }
    }

    suspend fun updateOriginAddress(address: OriginShippingAddress) {
        val addresses = observeOriginAddresses().first().orEmpty().toMutableList()
        val itemIndex = addresses.indexOfFirst { it.id == address.id }
        if (itemIndex != -1) {
            addresses[itemIndex] = address
        } else {
            addresses.add(address)
        }
        saveOriginAddresses(addresses)
    }

    suspend fun clearOriginAddresses() = dataStore.edit { it.remove(stringPreferencesKey(getOriginAddressesKey())) }
}
