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
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

class WooPosPreferencesRepositorySiteSwitchTest {
    private lateinit var selectedSite: SelectedSite
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repository: WooPosPreferencesRepository
    private lateinit var site1: SiteModel
    private lateinit var site2: SiteModel
    private lateinit var tempFile: Path

    @Before
    fun setup() {
        selectedSite = mock()
        tempFile = Files.createTempFile("woopos_prefs_test", ".preferences_pb")
        dataStore = PreferenceDataStoreFactory.createWithPath(
            produceFile = { tempFile.toString().toPath() }
        )

        site1 = SiteModel().apply { id = 1 }
        site2 = SiteModel().apply { id = 2 }

        whenever(selectedSite.getOrNull()).thenReturn(site1)
        repository = WooPosPreferencesRepository(selectedSite, dataStore)
    }

    @After
    fun tearDown() {
        try {
            Files.deleteIfExists(tempFile)
        } catch (_: Exception) {
            // Ignore cleanup errors
        }
    }

    @Test
    fun `given product searches saved for site1, when switching to site2, then site2 has empty searches`() = runTest {
        repository.addRecentProductSearch("product_site1")

        val site1Searches = repository.recentProductSearches.first()
        assertEquals(listOf("product_site1"), site1Searches)

        whenever(selectedSite.getOrNull()).thenReturn(site2)

        val site2SearchesBeforeAdding = repository.recentProductSearches.first()
        assertEquals(emptyList(), site2SearchesBeforeAdding)

        repository.addRecentProductSearch("product_site2")

        val site2SearchesAfterAdding = repository.recentProductSearches.first()
        assertEquals(listOf("product_site2"), site2SearchesAfterAdding)

        whenever(selectedSite.getOrNull()).thenReturn(site1)

        val site1SearchesAfterSwitchBack = repository.recentProductSearches.first()
        assertEquals(listOf("product_site1"), site1SearchesAfterSwitchBack)
    }

    @Test
    fun `given coupon searches saved for site1, when switching to site2, then site2 has empty searches`() = runTest {
        repository.addRecentCouponSearch("coupon_site1")

        val site1Searches = repository.recentCouponSearches.first()
        assertEquals(listOf("coupon_site1"), site1Searches)

        whenever(selectedSite.getOrNull()).thenReturn(site2)

        val site2SearchesBeforeAdding = repository.recentCouponSearches.first()
        assertEquals(emptyList(), site2SearchesBeforeAdding)

        repository.addRecentCouponSearch("coupon_site2")

        val site2SearchesAfterAdding = repository.recentCouponSearches.first()
        assertEquals(listOf("coupon_site2"), site2SearchesAfterAdding)

        whenever(selectedSite.getOrNull()).thenReturn(site1)

        val site1SearchesAfterSwitchBack = repository.recentCouponSearches.first()
        assertEquals(listOf("coupon_site1"), site1SearchesAfterSwitchBack)
    }

    @Test
    fun `given searches saved for multiple sites, when switching between sites, then each site shows only its own searches`() = runTest {
        repository.addRecentProductSearch("laptop")
        repository.addRecentCouponSearch("discount10")

        whenever(selectedSite.getOrNull()).thenReturn(site2)
        repository.addRecentProductSearch("phone")
        repository.addRecentCouponSearch("sale20")

        whenever(selectedSite.getOrNull()).thenReturn(site1)
        assertEquals(listOf("laptop"), repository.recentProductSearches.first())
        assertEquals(listOf("discount10"), repository.recentCouponSearches.first())

        whenever(selectedSite.getOrNull()).thenReturn(site2)
        assertEquals(listOf("phone"), repository.recentProductSearches.first())
        assertEquals(listOf("sale20"), repository.recentCouponSearches.first())
    }
}
