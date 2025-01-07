package com.woocommerce.android.ui.sitepicker.sitevisibility

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class VisibleWooSitesDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.SITE_PICKER_WOO_VISIBLE_SITES) private val dataStore: DataStore<Preferences>
) {
    suspend fun updateSiteVisibilityStatus(siteIds: Map<Long, Boolean>) {
        siteIds.forEach { (siteId, isVisible) ->
            updateSiteVisibility(siteId, isVisible)
        }
    }

    fun isSiteVisible(siteId: Long): Flow<Boolean> {
        return dataStore.data.map { prefs -> prefs[booleanPreferencesKey(siteId.toString())] != false }
    }

    suspend fun clearAll() {
        dataStore.edit { it.clear() }
    }

    private suspend fun updateSiteVisibility(siteId: Long, isVisible: Boolean) {
        dataStore.edit { preferences ->
            preferences[booleanPreferencesKey(siteId.toString())] = isVisible
        }
    }
}
