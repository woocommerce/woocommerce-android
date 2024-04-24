package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
) {
    fun isUserLoggedIn() = loginDataStore.data
        .map { it[stringPreferencesKey(STORE_CONFIG_KEY)].isNullOrEmpty().not() }

    suspend fun receiveStoreData(data: DataMap) {
        data.getString("token")?.let { token ->
            loginDataStore.edit { prefs ->
                prefs[stringPreferencesKey(STORE_CONFIG_KEY)] = token
            }
        }
    }

    companion object {
        const val STORE_CONFIG_KEY = "store_config_key"
    }
}
