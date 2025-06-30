package com.woocommerce.android.ui.orders.wooshippinglabels.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooShippingEligibilityDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SHIPPING_LABELS_DATA) private val dataStore: DataStore<Preferences>,
    private val selectedSite: SelectedSite,
) {
    private fun getEligibilityKey(orderId: Long) = "${selectedSite.get().id}:${orderId}Eligibility"

    fun observeEligibility(orderId: Long): Flow<Boolean?> = dataStore.data.map { prefs ->
        prefs[booleanPreferencesKey(getEligibilityKey(orderId))]
    }.distinctUntilChanged()

    suspend fun saveEligibility(orderId: Long, isEligible: Boolean?) {
        dataStore.edit { preferences ->
            if (isEligible == null) {
                preferences.remove(booleanPreferencesKey(getEligibilityKey(orderId)))
            } else {
                preferences[booleanPreferencesKey(getEligibilityKey(orderId))] = isEligible
            }
        }
    }
}
