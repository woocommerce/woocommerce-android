package com.woocommerce.android.tracker

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class DataStoreTrackerRepository @Inject constructor(
    @DataStoreQualifier(DataStoreType.TRACKER) private val dataStore: DataStore<Preferences>
) : TrackerRepository {
    override fun observeLastSendingDate(site: SiteModel): Flow<Long> {
        return dataStore.data.map { preferences ->
            preferences[longPreferencesKey(site.id.toString())] ?: 0
        }
    }

    override suspend fun updateLastSendingDate(site: SiteModel, lastUpdateMillis: Long) {
        dataStore.edit { trackerPreferences ->
            trackerPreferences[longPreferencesKey(site.id.toString())] = lastUpdateMillis
        }
    }
}
