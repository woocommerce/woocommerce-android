package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
) {
    fun isUserLoggedIn() = loginDataStore.data
        .map { it[stringPreferencesKey(generateStoreConfigKey())].isNullOrEmpty().not() }

    suspend fun receiveStoreData(data: DataMap) {
        data.getString("token")?.let { token ->
            loginDataStore.edit { prefs ->
                prefs[stringPreferencesKey(generateStoreConfigKey())] = token
            }
        }
    }

    private fun generateStoreConfigKey(): String {
        return "store_config_key"
    }
}
