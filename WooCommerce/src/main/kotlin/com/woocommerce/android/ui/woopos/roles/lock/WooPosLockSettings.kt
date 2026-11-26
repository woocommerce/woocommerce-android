package com.woocommerce.android.ui.woopos.roles.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject

/** Stores the Security lock choices per store on this device. Logout clears them with the POS data store. */
class WooPosLockSettings @Inject constructor(
    @DataStoreQualifier(DataStoreType.WOO_POS) private val dataStore: DataStore<Preferences>,
) {
    fun preferences(localSiteId: Int): Flow<WooPosLockPreferences> = dataStore.data
        .map { it.lockPreferences(localSiteId) }
        .catch { error ->
            if (error !is IOException) throw error
            WooLog.e(WooLog.T.POS, "Unable to read POS lock settings")
            emit(WooPosLockPreferences())
        }
        .distinctUntilChanged()

    suspend fun update(localSiteId: Int, preferences: WooPosLockPreferences): Boolean = try {
        dataStore.edit {
            it[locksWhenPOSOpensKey(localSiteId)] = preferences.locksWhenPOSOpens
            it[locksInBackgroundKey(localSiteId)] = preferences.locksInBackground
            it[locksAfterSaleKey(localSiteId)] = preferences.locksAfterSale
            it[inactivityTimeoutKey(localSiteId)] = preferences.inactivityTimeout.minutes
        }
        true
    } catch (e: IOException) {
        WooLog.e(WooLog.T.POS, "Unable to save POS lock settings: ${e.javaClass.simpleName}")
        false
    }

    private fun Preferences.lockPreferences(localSiteId: Int): WooPosLockPreferences {
        val defaults = WooPosLockPreferences()
        val timeout = this[inactivityTimeoutKey(localSiteId)]
        return WooPosLockPreferences(
            locksWhenPOSOpens = this[locksWhenPOSOpensKey(localSiteId)] ?: defaults.locksWhenPOSOpens,
            locksInBackground = this[locksInBackgroundKey(localSiteId)] ?: defaults.locksInBackground,
            locksAfterSale = this[locksAfterSaleKey(localSiteId)] ?: defaults.locksAfterSale,
            inactivityTimeout = WooPosInactivityLockTimeout.entries.firstOrNull { it.minutes == timeout }
                ?: defaults.inactivityTimeout,
        )
    }

    private companion object {
        fun locksWhenPOSOpensKey(localSiteId: Int) = booleanPreferencesKey("${localSiteId}_pos_locks_when_pos_opens")
        fun locksInBackgroundKey(localSiteId: Int) = booleanPreferencesKey("${localSiteId}_pos_locks_in_background")
        fun locksAfterSaleKey(localSiteId: Int) = booleanPreferencesKey("${localSiteId}_pos_locks_after_sale")
        fun inactivityTimeoutKey(localSiteId: Int) = intPreferencesKey("${localSiteId}_pos_inactivity_lock_minutes")
    }
}
