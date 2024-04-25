package com.woocommerce.android.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.commons.wear.DataParameters.SITE_JSON
import com.woocommerce.commons.wear.DataParameters.TOKEN
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import kotlinx.coroutines.flow.combine
import org.wordpress.android.fluxc.model.SiteModel

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
) {
    private val gson by lazy { Gson() }

    val currentSite
        get() = loginDataStore.data
            .map { it[stringPreferencesKey(CURRENT_SITE_KEY)] }
            .map { it?.let { gson.fromJson(it, SiteModel::class.java) } }

    val isUserLoggedIn
        get() = combine(loginDataStore.data, currentSite) { prefs, site ->
            site?.takeIf { it.siteId > 0 }
                ?.let { prefs[stringPreferencesKey(generateTokenKey(it.siteId))] }
                ?.isNotEmpty()
                ?: false
        }

    suspend fun receiveStoreData(data: DataMap) {
        val siteJSON = data.getString(SITE_JSON.value)
        val site = siteJSON
            ?.let { gson.fromJson(it, SiteModel::class.java) }
            ?: return

        loginDataStore.edit { prefs ->
            prefs[stringPreferencesKey(CURRENT_SITE_KEY)] = siteJSON
            data.getString(TOKEN.value)?.let { token ->
                prefs[stringPreferencesKey(generateTokenKey(site.siteId))] = token
            }
        }
    }

    private fun generateTokenKey(siteId: Long) = "$TOKEN_KEY:$siteId"

    companion object {
        const val CURRENT_SITE_KEY = "current_site_key"
        const val TOKEN_KEY = "token_key"
    }
}
