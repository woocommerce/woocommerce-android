package com.woocommerce.android.ui.woopos.tab

import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosSupportedCountriesTest : BaseUnitTest() {

    private val featureFlagRepository: FeatureFlagRepository = mock()

    private lateinit var sut: WooPosSupportedCountries

    @Before
    fun setup() = testBlocking {
        whenever(featureFlagRepository.awaitRemoteFlagsLoaded()).thenReturn(Unit)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(false)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(false)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_AUSTRALIA_WOOPAYMENTS)).thenReturn(false)
        sut = WooPosSupportedCountries(featureFlagRepository)
    }

    @Test
    fun `given both flags off, when supportedCountryCurrencyPairs called, then base pairs only returned`() = testBlocking {
        assertThat(sut.supportedCountryCurrencyPairs())
            .containsExactlyInAnyOrder("us" to "usd", "pr" to "usd", "gb" to "gbp")
    }

    @Test
    fun `given both flags off, when supportedCountryCurrencyPairs called, then PR is included alongside US`() = testBlocking {
        assertThat(sut.supportedCountryCurrencyPairs()).contains("pr" to "usd")
    }

    @Test
    fun `given primary flag on, when supportedCountryCurrencyPairs called, then base plus primary group returned`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)

        assertThat(sut.supportedCountryCurrencyPairs())
            .containsExactlyInAnyOrder(
                "us" to "usd",
                "pr" to "usd",
                "gb" to "gbp",
                "fr" to "eur",
                "de" to "eur",
                "ie" to "eur",
                "nl" to "eur",
                "sg" to "sgd",
                "nz" to "nzd",
            )
    }

    @Test
    fun `given EU extended flag on, when supportedCountryCurrencyPairs called, then base plus EU extended group returned`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)

        assertThat(sut.supportedCountryCurrencyPairs())
            .containsExactlyInAnyOrder(
                "us" to "usd", "pr" to "usd", "gb" to "gbp",
                "at" to "eur", "be" to "eur", "fi" to "eur", "it" to "eur",
                "lu" to "eur", "pt" to "eur", "es" to "eur",
            )
    }

    @Test
    fun `given primary and EU extended flags on, when supportedCountryCurrencyPairs called, then all 16 pairs returned and AU absent`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)

        val pairs = sut.supportedCountryCurrencyPairs()
        assertThat(pairs).hasSize(16)
        assertThat(pairs.map { it.first }).doesNotContain("au")
    }

    @Test
    fun `given AU flag on, when supportedCountryCurrencyPairs called, then base plus AU returned`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_AUSTRALIA_WOOPAYMENTS)).thenReturn(true)

        assertThat(sut.supportedCountryCurrencyPairs())
            .containsExactlyInAnyOrder(
                "us" to "usd",
                "pr" to "usd",
                "gb" to "gbp",
                "au" to "aud",
            )
    }

    @Test
    fun `given all expansion flags on, when supportedCountries called, then all country codes returned`() = testBlocking {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_AUSTRALIA_WOOPAYMENTS)).thenReturn(true)

        assertThat(sut.supportedCountries())
            .containsExactlyInAnyOrder(
                "us", "pr", "gb",
                "fr", "de", "ie", "nl", "sg", "nz",
                "at", "be", "fi", "it", "lu", "pt", "es",
                "au",
            )
    }
}
