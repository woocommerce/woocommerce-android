package com.woocommerce.android.ui.woopos.roles.lock

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import java.nio.file.Files

class WooPosLockSettingsTest {
    private val file = Files.createTempFile("woopos_lock_settings_test", ".preferences_pb").also {
        Files.deleteIfExists(it)
    }
    private val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
        produceFile = { file.toString().toPath() }
    )
    private val lockSettings = WooPosLockSettings(dataStore)

    @After
    fun tearDown() {
        Files.deleteIfExists(file)
    }

    @Test
    fun `given nothing saved, when reading, then the iOS defaults apply`() = runTest {
        assertThat(lockSettings.preferences(SITE_ID).first()).isEqualTo(
            WooPosLockPreferences(
                locksWhenPOSOpens = true,
                locksInBackground = true,
                locksAfterSale = false,
                inactivityTimeout = WooPosInactivityLockTimeout.FIVE_MINUTES,
            )
        )
    }

    @Test
    fun `when preferences are saved, then they are read back for that store only`() = runTest {
        // GIVEN
        val saved = WooPosLockPreferences(
            locksWhenPOSOpens = false,
            locksInBackground = false,
            locksAfterSale = true,
            inactivityTimeout = WooPosInactivityLockTimeout.NEVER,
        )

        // WHEN
        val result = lockSettings.update(SITE_ID) { saved }

        // THEN
        assertThat(result.getOrNull()).isEqualTo(saved)
        assertThat(lockSettings.preferences(SITE_ID).first()).isEqualTo(saved)
        assertThat(lockSettings.preferences(SITE_ID + 1).first()).isEqualTo(WooPosLockPreferences())
    }

    @Test
    fun `given two quick changes, when both are saved, then each keeps the other`() = runTest {
        // WHEN
        lockSettings.update(SITE_ID) { it.copy(locksAfterSale = true) }
        lockSettings.update(SITE_ID) { it.copy(locksInBackground = false) }

        // THEN
        assertThat(lockSettings.preferences(SITE_ID).first())
            .isEqualTo(WooPosLockPreferences(locksAfterSale = true, locksInBackground = false))
    }

    @Test
    fun `given an unknown saved timeout, when reading, then the default timeout applies`() = runTest {
        // GIVEN
        dataStore.edit { it[intPreferencesKey("${SITE_ID}_pos_inactivity_lock_minutes")] = 7 }

        // WHEN
        val preferences = lockSettings.preferences(SITE_ID).first()

        // THEN
        assertThat(preferences.inactivityTimeout).isEqualTo(WooPosInactivityLockTimeout.FIVE_MINUTES)
    }

    private companion object {
        const val SITE_ID = 1
    }
}
