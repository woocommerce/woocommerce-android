package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class WooPosIsCountryAllowedTest {

    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val featureFlagRepository: FeatureFlagRepository = mock()

    private val sut = WooPosIsCountryAllowed(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        featureFlagRepository = featureFlagRepository,
    )

    @Test
    fun `given all-countries flag enabled, when invoked, then returns true regardless of country`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(true)
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("JP")

        assertThat(sut()).isTrue
    }

    @Test
    fun `given flag disabled and supported country, when invoked, then returns true`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("US")

        assertThat(sut()).isTrue
    }

    @Test
    fun `given flag disabled and POS-supported country, when invoked, then returns true for each supported country`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)

        listOf("US", "PR", "GB", "IE", "NL", "SG", "NZ", "FI", "LU", "AU").forEach { countryCode ->
            whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn(countryCode)

            assertThat(sut())
                .describedAs("$countryCode should be allowed")
                .isTrue
        }
    }

    @Test
    fun `given flag disabled and lowercase country code, when invoked, then matches case-insensitively`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("gb")

        assertThat(sut()).isTrue
    }

    @Test
    fun `given flag disabled and unsupported country, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("JP")

        assertThat(sut()).isFalse
    }

    @Test
    fun `given flag disabled and fiscalization-only country, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)

        listOf("AT", "BE", "FR", "IT", "DE", "PT", "ES").forEach { countryCode ->
            whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn(countryCode)

            assertThat(sut())
                .describedAs("$countryCode should not be allowed")
                .isFalse
        }
    }

    @Test
    fun `given flag disabled and no selected site, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut()).isFalse
    }

    @Test
    fun `given flag disabled and site without country code, when invoked, then returns false`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.WOO_POS_ALL_COUNTRIES)).thenReturn(false)
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn(null)

        assertThat(sut()).isFalse
    }
}
