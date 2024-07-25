package com.woocommerce.android.background

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.utils.CurrentTimeProvider
import javax.inject.Inject

/***
 * Data store responsible for signaling when the cached data should be updated
 * through stored timestamp.
 */
class LastUpdateDataStore @Inject constructor(
    @DataStoreQualifier(DataStoreType.LAST_UPDATE) private val dataStore: DataStore<Preferences>,
    private val currentTimeProvider: CurrentTimeProvider,
    private val selectedSite: SelectedSite
) {
    fun shouldUpdateData(
        key: String,
        maxOutdatedTime: Long = DEFAULT_MAX_OUTDATED_TIME,
    ) = dataStore.data
        .map { prefs -> prefs[longPreferencesKey(key)] }
        .map { lastUpdateTime -> isElapsedTimeExpired(lastUpdateTime, maxOutdatedTime) }

    suspend fun storeLastUpdate(key: String) {
        dataStore.edit { preferences ->
            preferences[longPreferencesKey(key)] = currentTime
        }
    }

    private fun getLastUpdateKeyByDataType(data: UpdateData) = "${selectedSite.getSelectedSiteId()}-${data.name}"

    fun getLastUpdateKeyByOrdersListId(listId: Int) = "${getLastUpdateKeyByDataType(UpdateData.ORDERS)}-$listId"

    private fun isElapsedTimeExpired(lastUpdateTime: Long?, maxOutdatedTime: Long): Boolean =
        lastUpdateTime?.let {
            (currentTime - it) > maxOutdatedTime
        } ?: true

    fun observeLastUpdate(key: String): Flow<Long?> {
        return dataStore.data.map { prefs -> prefs[longPreferencesKey(key)] }
    }

    private val currentTime
        get() = currentTimeProvider.currentDate().time

    companion object {
        const val DEFAULT_MAX_OUTDATED_TIME = 1000 * 60 * 30L // 30 minutes
    }

    enum class UpdateData {
        ORDERS
    }
}
