package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataItem
import com.google.android.gms.wearable.DataMapItem
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import javax.inject.Inject
import kotlinx.coroutines.flow.map

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
) {
    fun isUserLoggedIn() = loginDataStore.data
        .map { it[stringPreferencesKey(generateStoreConfigKey())].isNullOrEmpty().not() }

    fun receiveStoreData(data: DataMapItem) {

    }

    private fun generateStoreConfigKey(): String {
        return "store_config_key"
    }
}
