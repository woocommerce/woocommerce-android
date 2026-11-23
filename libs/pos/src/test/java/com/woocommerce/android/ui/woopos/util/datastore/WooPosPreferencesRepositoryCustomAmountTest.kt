package com.woocommerce.android.ui.woopos.util.datastore

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

class WooPosPreferencesRepositoryCustomAmountTest {
    @Rule
    @JvmField
    val temporaryFolder = TemporaryFolder()

    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock()

    @Test
    fun `given no saved choice, when read, then charge taxes is off`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(site)
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFolder().resolve("pos.preferences_pb")
        }
        val sut = WooPosPreferencesRepository(selectedSite, dataStore)

        // THEN
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
    }

    @Test
    fun `given a saved choice, when replaced, then the latest choice is returned`() = runTest {
        // GIVEN
        whenever(selectedSite.getOrNull()).thenReturn(site)
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFolder().resolve("pos.preferences_pb")
        }
        val sut = WooPosPreferencesRepository(selectedSite, dataStore)
        sut.setCustomAmountLastTaxable(true)
        assertThat(sut.getCustomAmountLastTaxable()).isTrue()

        // WHEN
        sut.setCustomAmountLastTaxable(false)

        // THEN
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
    }

    @Test
    fun `given different choices per store, when switching stores, then each choice is preserved`() = runTest {
        // GIVEN
        val otherSite = SiteModel().apply { id = 2 }
        whenever(selectedSite.getOrNull()).thenReturn(site)
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFolder().resolve("pos.preferences_pb")
        }
        val sut = WooPosPreferencesRepository(selectedSite, dataStore)
        sut.setCustomAmountLastTaxable(true)

        // WHEN
        whenever(selectedSite.getOrNull()).thenReturn(otherSite)
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
        sut.setCustomAmountLastTaxable(false)
        whenever(selectedSite.getOrNull()).thenReturn(site)

        // THEN
        assertThat(sut.getCustomAmountLastTaxable()).isTrue()
    }

    @Test
    fun `given no selected store, when saving, then nothing is stored and charge taxes is off`() = runTest {
        // GIVEN
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFolder().resolve("pos.preferences_pb")
        }
        val sut = WooPosPreferencesRepository(selectedSite, dataStore)

        // WHEN
        sut.setCustomAmountLastTaxable(true)

        // THEN
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
        whenever(selectedSite.getOrNull()).thenReturn(site)
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
    }

    @Test
    fun `given saved choices for multiple stores, when datastore is cleared at logout, then all choices reset`() = runTest {
        // GIVEN
        val otherSite = SiteModel().apply { id = 2 }
        whenever(selectedSite.getOrNull()).thenReturn(site)
        val dataStore = PreferenceDataStoreFactory.create(scope = backgroundScope) {
            temporaryFolder.newFolder().resolve("pos.preferences_pb")
        }
        val sut = WooPosPreferencesRepository(selectedSite, dataStore)
        sut.setCustomAmountLastTaxable(true)
        whenever(selectedSite.getOrNull()).thenReturn(otherSite)
        sut.setCustomAmountLastTaxable(true)

        // WHEN
        dataStore.edit { it.clear() }

        // THEN
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
        whenever(selectedSite.getOrNull()).thenReturn(site)
        assertThat(sut.getCustomAmountLastTaxable()).isFalse()
    }
}
