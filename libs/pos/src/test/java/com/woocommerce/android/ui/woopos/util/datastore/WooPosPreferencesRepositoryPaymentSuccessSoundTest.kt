package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class WooPosPreferencesRepositoryPaymentSuccessSoundTest {
    private lateinit var tempFile: Path
    private lateinit var repository: WooPosPreferencesRepository

    @Before
    fun setup() {
        tempFile = Files.createTempFile("woopos_prefs_sound_test", ".preferences_pb")
        Files.deleteIfExists(tempFile)
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.toString().toPath() }
        )
        repository = WooPosPreferencesRepository(mock<SelectedSite>(), dataStore)
    }

    @After
    fun tearDown() {
        Files.deleteIfExists(tempFile)
    }

    @Test
    fun `given nothing stored, when reading payment success sound, then it is enabled`() = runTest {
        assertTrue(repository.isPaymentSuccessSoundEnabled.first())
    }

    @Test
    fun `when payment success sound disabled, then it is reported as disabled`() = runTest {
        repository.setPaymentSuccessSoundEnabled(false)

        assertFalse(repository.isPaymentSuccessSoundEnabled.first())
    }

    @Test
    fun `given sound disabled, when enabled again, then it is reported as enabled`() = runTest {
        repository.setPaymentSuccessSoundEnabled(false)

        repository.setPaymentSuccessSoundEnabled(true)

        assertTrue(repository.isPaymentSuccessSoundEnabled.first())
    }
}
