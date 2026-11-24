package com.woocommerce.android.ui.woopos.roles.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosLockPreferencesTest {
    @get:Rule val temporaryFolder = TemporaryFolder()

    @Test
    fun `given saved settings, when the data store reopens, then each store retains its own choices`() = runTest {
        // GIVEN
        val file = temporaryFolder.newFolder().resolve("locks.preferences_pb")
        val writer = Job()
        val first = PreferenceDataStoreFactory.create(scope = CoroutineScope(coroutineContext + writer)) { file }
        val preferences = WooPosLockPreferences(first)
        val changed = WooPosAutoLockSettings(false, false, true, 15)
        assertThat(preferences.updateSettings(1, { true }) { changed }.isSuccess).isTrue()
        writer.cancelAndJoin()

        // WHEN
        val reopened = PreferenceDataStoreFactory.create(scope = backgroundScope) { file }
        val restored = WooPosLockPreferences(reopened)

        // THEN
        assertThat(restored.settings(1).first().getOrThrow()).isEqualTo(changed)
        assertThat(restored.settings(2).first().getOrThrow()).isEqualTo(WooPosAutoLockSettings())
    }

    @Test
    fun `given retired authority, when settings or restoration are saved, then storage stays unchanged`() = runTest {
        // GIVEN
        val preferences = WooPosLockPreferences(dataStore(backgroundScope))

        // WHEN
        val setting = preferences.updateSettings(1, { false }) { it.copy(onBackground = false) }
        val restoration = preferences.retainPOS(1) { false }

        // THEN
        assertThat(setting.isFailure).isTrue()
        assertThat(restoration.isFailure).isTrue()
        assertThat(preferences.settings(1).first().getOrThrow().onBackground).isTrue()
        assertThat(preferences.restoreSiteId().getOrThrow()).isNull()
    }

    @Test
    fun `given invalid saved inactivity, when settings are read, then the read fails closed`() = runTest {
        // GIVEN
        val dataStore = dataStore(backgroundScope)
        dataStore.edit { it[intPreferencesKey("1_lock_inactivity_minutes")] = -1 }

        // THEN
        assertThat(WooPosLockPreferences(dataStore).settings(1).first().isFailure).isTrue()
    }

    @Test
    fun `given an active POS marker, when exit clears it, then another reader does not restore POS`() = runTest {
        // GIVEN
        val dataStore = dataStore(backgroundScope)
        val preferences = WooPosLockPreferences(dataStore)
        preferences.retainPOS(1) { true }.getOrThrow()
        assertThat(WooPosLockPreferences(dataStore).restoreSiteId().getOrThrow()).isEqualTo(1)

        // WHEN
        preferences.clearRestoreState().getOrThrow()

        // THEN
        assertThat(WooPosLockPreferences(dataStore).restoreSiteId().getOrThrow()).isNull()
    }

    private fun dataStore(scope: CoroutineScope): DataStore<Preferences> {
        val file = temporaryFolder.newFolder().resolve("locks.preferences_pb")
        return PreferenceDataStoreFactory.create(scope = scope) { file }
    }
}
