package com.woocommerce.android.ui.woopos.roles.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.woocommerce.android.datastore.DataStoreQualifier
import com.woocommerce.android.datastore.DataStoreType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class WooPosLockPreferences @Inject constructor(
    @DataStoreQualifier(DataStoreType.WOO_POS_LOCK_SETTINGS) private val dataStore: DataStore<Preferences>,
) {
    fun settings(localSiteId: Int): Flow<Result<WooPosAutoLockSettings>> = dataStore.data.map { preferences ->
        result { preferences.settings(localSiteId) }
    }.distinctUntilChanged().catch { error ->
        if (error is CancellationException) throw error
        emit(Result.failure(error))
    }

    suspend fun updateSettings(
        localSiteId: Int,
        canCommit: () -> Boolean,
        update: (WooPosAutoLockSettings) -> WooPosAutoLockSettings,
    ): Result<Unit> = result {
        dataStore.edit { preferences ->
            check(canCommit())
            val settings = update(preferences.settings(localSiteId))
            preferences[booleanPreferencesKey("${localSiteId}_lock_on_launch")] = settings.onLaunch
            preferences[booleanPreferencesKey("${localSiteId}_lock_on_background")] = settings.onBackground
            preferences[booleanPreferencesKey("${localSiteId}_lock_after_sale")] = settings.afterSale
            preferences[intPreferencesKey("${localSiteId}_lock_inactivity_minutes")] = settings.inactivityMinutes
        }
    }

    suspend fun restoreSiteId(): Result<Int?> = result {
        dataStore.data.first()[RESTORE_SITE_ID].also { require(it == null || it > 0) }
    }

    suspend fun retainPOS(localSiteId: Int, canCommit: () -> Boolean): Result<Unit> = result {
        dataStore.edit { preferences ->
            check(canCommit())
            preferences[RESTORE_SITE_ID] = localSiteId
        }
    }

    suspend fun clearRestoreState(): Result<Unit> = clearRestoreState(null) { true }

    suspend fun clearAll(): Result<Unit> = result {
        dataStore.edit { it.clear() }
    }

    suspend fun clearRestoreState(localSiteId: Int?, canCommit: () -> Boolean): Result<Unit> = result {
        dataStore.edit {
            check(canCommit())
            if (localSiteId == null || it[RESTORE_SITE_ID] == localSiteId) it.remove(RESTORE_SITE_ID)
        }
    }

    private fun Preferences.settings(id: Int) = WooPosAutoLockSettings(
        onLaunch = this[booleanPreferencesKey("${id}_lock_on_launch")] ?: true,
        onBackground = this[booleanPreferencesKey("${id}_lock_on_background")] ?: true,
        afterSale = this[booleanPreferencesKey("${id}_lock_after_sale")] ?: false,
        inactivityMinutes = this[intPreferencesKey("${id}_lock_inactivity_minutes")] ?: 5,
    )

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> result(action: () -> T): Result<T> = try {
        Result.success(action())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Result.failure(error)
    }

    companion object {
        private val RESTORE_SITE_ID = intPreferencesKey("pos_restore_site_id")
    }
}
