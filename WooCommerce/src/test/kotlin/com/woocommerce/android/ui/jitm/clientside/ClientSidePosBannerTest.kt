package com.woocommerce.android.ui.jitm.clientside

import android.content.Context
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.WooPosIsScreenSizeAllowed
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils

@ExperimentalCoroutinesApi
class ClientSidePosBannerTest : BaseUnitTest() {

    private val context: Context = mock()
    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val wooPosIsScreenSizeAllowed: WooPosIsScreenSizeAllowed = mock()
    private val dismissalStorage: ClientSideBannerDismissalStorage = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private lateinit var sut: ClientSidePosBanner

    @Before
    fun setup() = testBlocking {
        whenever(featureFlagRepository.isEnabled(any())).thenReturn(true)
        sut = ClientSidePosBanner(
            context = context,
            selectedSite = selectedSite,
            wooStore = wooStore,
            wooPosIsScreenSizeAllowed = wooPosIsScreenSizeAllowed,
            dismissalStorage = dismissalStorage,
            featureFlagRepository = featureFlagRepository,
        )
    }

    @Test
    fun `given no site selected, when shouldShow called, then returns false`() = testBlocking {
        whenever(selectedSite.getIfExists()).thenReturn(null)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given client banner feature flag is disabled, when shouldShow called, then returns false`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_TABLET_PROMO_BANNER)).thenReturn(false)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
        verify(featureFlagRepository).isEnabled(FeatureFlag.WOO_POS_TABLET_PROMO_BANNER)
        verify(selectedSite, never()).getIfExists()
    }

    @Test
    fun `given tablet device, when shouldShow called, then returns false`() = testBlocking {
        setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(true)

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given non-eligible country, when shouldShow called, then returns false`() = testBlocking {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getSiteSettingsAsync(site)).thenReturn(settingsWithCountry("CA"))

        val result = sut.shouldShow()

        assertThat(result).isFalse()
    }

    @Test
    fun `given all conditions met, when shouldShow called, then returns true`() = testBlocking {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getSiteSettingsAsync(site)).thenReturn(settingsWithCountry("US"))
        whenever(dismissalStorage.isBannerHidden(any(), any())).thenReturn(false)

        val result = sut.shouldShow()

        assertThat(result).isTrue()
    }

    @Test
    fun `when onDismiss called, then banner is hidden in storage`() {
        val site = setupValidSite()

        sut.onDismiss()

        verify(dismissalStorage).hideBanner("woo_pos_client_banner", site)
    }

    @Test
    fun `given all conditions met, when shouldShow called, then checks the client banner feature flag`() = testBlocking {
        val site = setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(wooStore.getSiteSettingsAsync(site)).thenReturn(settingsWithCountry("US"))
        whenever(dismissalStorage.isBannerHidden(any(), any())).thenReturn(false)

        sut.shouldShow()

        verify(featureFlagRepository).isEnabled(eq(FeatureFlag.WOO_POS_TABLET_PROMO_BANNER))
    }

    @Test
    fun `given all conditions met, when shouldShow called, then the country code is not read on the main thread`() =
        testBlocking {
            // GIVEN
            val site = setupValidSite()
            whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
            whenever(wooStore.getSiteSettingsAsync(site)).thenReturn(settingsWithCountry("US"))
            whenever(dismissalStorage.isBannerHidden(any(), any())).thenReturn(false)

            // WHEN
            sut.shouldShow()

            // THEN
            verify(wooStore, never()).getStoreCountryCode(any())
        }

    @Test
    fun `given banner already dismissed, when shouldShow called, then site settings are not read`() = testBlocking {
        // GIVEN
        setupValidSite()
        whenever(wooPosIsScreenSizeAllowed()).thenReturn(false)
        whenever(dismissalStorage.isBannerHidden(any(), any())).thenReturn(true)

        // WHEN
        val result = sut.shouldShow()

        // THEN
        assertThat(result).isFalse()
        verify(wooStore, never()).getSiteSettingsAsync(any())
    }

    private fun setupValidSite(): SiteModel {
        val site = SiteModel().apply {
            id = 1
            siteId = 99L
            selfHostedSiteId = 0L
        }
        whenever(selectedSite.getIfExists()).thenReturn(site)
        return site
    }

    private fun settingsWithCountry(countryCode: String) =
        WCSettingsTestUtils.generateSettings(LocalId(1)).copy(countryCode = countryCode)
}
