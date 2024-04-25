package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.commons.wear.DataParameters.SITE_DATA
import com.woocommerce.commons.wear.DataParameters.TOKEN
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import org.wordpress.android.fluxc.model.SiteModel

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
) {
    private val gson by lazy { Gson() }

    fun isUserLoggedIn() = loginDataStore.data
        .map { it[stringPreferencesKey(STORE_CONFIG_KEY)].isNullOrEmpty().not() }

    suspend fun receiveStoreData(data: DataMap) {
        val site = data.getString(SITE_DATA.value)
            ?.let { gson.fromJson(it, SiteModel::class.java) }

        data.getString(TOKEN.value)?.let { token ->
            loginDataStore.edit { prefs ->
                prefs[stringPreferencesKey(STORE_CONFIG_KEY)] = token
            }
        }
    }

    companion object {
        const val STORE_CONFIG_KEY = "store_config_key"
    }
}
