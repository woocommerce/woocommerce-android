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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.AccountActionBuilder.newUpdateAccessTokenAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.AccountStore.UpdateTokenPayload
import javax.inject.Inject

class LoginRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.LOGIN) private val loginDataStore: DataStore<Preferences>,
    private val dispatcher: Dispatcher,
    coroutineScope: CoroutineScope
) {
    private val gson by lazy { Gson() }

    private val siteFlow: MutableStateFlow<SiteModel?> = MutableStateFlow(null)
    val selectedSiteFlow: StateFlow<SiteModel?> = siteFlow
    val isSiteAvailable = siteFlow.map { it != null && it.siteId > 0 }

    val currentSite: SiteModel?
        get() = siteFlow.value

    init {
        loginDataStore.data
            .map { it[stringPreferencesKey(CURRENT_SITE_KEY)] }
            .map { it?.let { gson.fromJson(it, SiteModel::class.java) } }
            .onEach { siteFlow.value = it }
            .launchIn(coroutineScope)
    }

    suspend fun receiveStoreData(data: DataMap) {
        val siteJSON = data.getString(SITE_JSON.value)
        val site = siteJSON
            ?.let { gson.fromJson(it, SiteModel::class.java) }
            ?: return

        loginDataStore.edit { prefs ->
            prefs[stringPreferencesKey(CURRENT_SITE_KEY)] = siteJSON
            data.getString(TOKEN.value)?.let { token ->
                dispatcher.dispatch(newUpdateAccessTokenAction(UpdateTokenPayload(token)))
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
