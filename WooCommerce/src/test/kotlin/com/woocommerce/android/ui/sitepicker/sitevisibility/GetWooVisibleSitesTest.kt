package com.woocommerce.android.ui.sitepicker.sitevisibility

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.sitepicker.SitePickerRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel

@OptIn(ExperimentalCoroutinesApi::class)
class GetWooVisibleSitesTest : BaseUnitTest() {
    private val sitePickerRepository: SitePickerRepository = mock()
    private val visibleSitesDataStore: VisibleWooSitesDataStore = mock {
        on { isSiteVisible(any()) } doReturn flowOf(true)
    }
    private val selectedSite: SelectedSite = mock()

    private val wpComVisibleWooSite = SiteModel().apply {
        id = 1
        siteId = 101L
        origin = SiteModel.ORIGIN_WPCOM_REST
        hasWooCommerce = true
    }
    private val appPasswordWooSite = SiteModel().apply {
        id = 2
        siteId = 0L
        origin = SiteModel.ORIGIN_WPAPI
        hasWooCommerce = true
    }

    @Test
    fun `given visible wpcom woo sites, when invoked, then returns visible woo sites`() = testBlocking {
        // GIVEN
        val hiddenWooSite = SiteModel().apply {
            id = 3
            siteId = 202L
            origin = SiteModel.ORIGIN_WPCOM_REST
            hasWooCommerce = true
        }
        val nonWooSite = SiteModel().apply {
            id = 4
            siteId = 303L
            origin = SiteModel.ORIGIN_WPCOM_REST
            hasWooCommerce = false
        }
        val visibleSitesDataStore = mock<VisibleWooSitesDataStore> {
            on { isSiteVisible(101L) } doReturn flowOf(true)
            on { isSiteVisible(202L) } doReturn flowOf(false)
        }
        val sut = GetWooVisibleSites(sitePickerRepository, visibleSitesDataStore, selectedSite)
        doReturn(listOf(wpComVisibleWooSite, hiddenWooSite, nonWooSite)).whenever(sitePickerRepository).getSites()
        doReturn(null).whenever(selectedSite).getOrNull()

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).containsExactly(wpComVisibleWooSite)
    }

    @Test
    fun `given selected app password woo site, when invoked, then includes current selected site`() = testBlocking {
        // GIVEN
        val sut = GetWooVisibleSites(sitePickerRepository, visibleSitesDataStore, selectedSite)
        doReturn(listOf(wpComVisibleWooSite)).whenever(sitePickerRepository).getSites()
        doReturn(appPasswordWooSite).whenever(selectedSite).getOrNull()

        // WHEN
        val result = sut()

        // THEN
        assertThat(result).containsExactly(wpComVisibleWooSite, appPasswordWooSite)
    }

    @Test
    fun `given selected app password woo site already present, when invoked, then does not duplicate current site`() =
        testBlocking {
            // GIVEN
            val sut = GetWooVisibleSites(sitePickerRepository, visibleSitesDataStore, selectedSite)
            doReturn(listOf(wpComVisibleWooSite, appPasswordWooSite)).whenever(sitePickerRepository).getSites()
            doReturn(appPasswordWooSite).whenever(selectedSite).getOrNull()

            // WHEN
            val result = sut()

            // THEN
            assertThat(result).containsExactly(wpComVisibleWooSite, appPasswordWooSite)
        }
}
