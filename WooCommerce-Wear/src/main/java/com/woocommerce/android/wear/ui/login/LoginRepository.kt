package com.woocommerce.android.wear.ui.login

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.android.gms.wearable.DataMap
import com.google.gson.Gson
import com.woocommerce.android.wear.datastore.DataStoreQualifier
import com.woocommerce.android.wear.datastore.DataStoreType
import com.woocommerce.commons.DataParameters.SITE_JSON
import com.woocommerce.commons.DataParameters.TOKEN
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCWearableStore
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
    private val wearableStore: WCWearableStore,
    private val wooCommerceStore: WooCommerceStore,
    private val coroutineScope: CoroutineScope
) {
    private val gson by lazy { Gson() }

    private val siteFlow: MutableStateFlow<SiteModel?> = MutableStateFlow(null)
    val selectedSiteFlow: StateFlow<SiteModel?> = siteFlow
    val selectedSite get() = selectedSiteFlow.value
    val isSiteAvailable = siteFlow.map { it != null && it.siteId > 0 }

    init {
        loginDataStore.data
            .map { it[stringPreferencesKey(CURRENT_SITE_KEY)] }
            .map { it?.let { gson.fromJson(it, SiteModel::class.java) } }
            .onEach { siteFlow.value = it }
            .launchIn(coroutineScope)
    }

    suspend fun receiveStoreDataFromPhone(data: DataMap) {
        val siteJSON = data.getString(SITE_JSON.value)
        val site = siteJSON
            ?.let { gson.fromJson(it, SiteModel::class.java) }
            ?: return

        coroutineScope.async {
            wooCommerceStore.fetchSiteGeneralSettings(site)
        }.await()

        loginDataStore.edit { prefs ->
            prefs[stringPreferencesKey(CURRENT_SITE_KEY)] = siteJSON
            data.getString(TOKEN.value)?.let { token ->
                wearableStore.updateToken(token)
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
