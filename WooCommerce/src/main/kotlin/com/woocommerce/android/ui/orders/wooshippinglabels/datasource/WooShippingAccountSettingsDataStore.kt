package com.woocommerce.android.ui.orders.wooshippinglabels.datasource

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AccountSettingsModel
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooShippingAccountSettingsDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SHIPPING_LABELS_DATA) private val dataStore: DataStore<Preferences>,
    private val gson: Gson,
    private val selectedSite: SelectedSite
) {
    private fun getPrefKey() = "${selectedSite.get().id}AccountSettings"

    fun observeAccountSettings(): Flow<AccountSettingsModel?> {
        return dataStore.data.map { prefs ->
            prefs.getAccountSettings()
        }.distinctUntilChanged()
    }

    suspend fun saveAccountSettings(accountSettings: AccountSettingsModel) {
        dataStore.edit { preferences ->
            preferences[stringPreferencesKey(getPrefKey())] = gson.toJson(accountSettings)
        }
    }

    suspend fun updateAccountSettings(update: (AccountSettingsModel) -> AccountSettingsModel) {
        dataStore.edit { preferences ->
            val accountSettings = preferences.getAccountSettings()
            if (accountSettings == null) {
                WooLog.w(WooLog.T.SHIPPING_LABELS, "Account settings not found while trying to update local copy")
                return@edit
            }

            val updatedAccountSettings = update(accountSettings)
            preferences[stringPreferencesKey(getPrefKey())] = gson.toJson(updatedAccountSettings)
        }
    }

    private fun Preferences.getAccountSettings(): AccountSettingsModel? {
        return runCatching {
            val accountSettings = get(stringPreferencesKey(getPrefKey()))
            gson.fromJson(accountSettings, AccountSettingsModel::class.java)
        }.getOrNull()
    }
}
