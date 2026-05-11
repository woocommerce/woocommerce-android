package com.woocommerce.android.ui.payments.cardreader

import com.woocommerce.android.cardreader.config.CardReaderConfigFactory
import com.woocommerce.android.cardreader.config.CardReaderConfigForAT
import com.woocommerce.android.cardreader.config.CardReaderConfigForBE
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForDE
import com.woocommerce.android.cardreader.config.CardReaderConfigForES
import com.woocommerce.android.cardreader.config.CardReaderConfigForFI
import com.woocommerce.android.cardreader.config.CardReaderConfigForFR
import com.woocommerce.android.cardreader.config.CardReaderConfigForGB
import com.woocommerce.android.cardreader.config.CardReaderConfigForIE
import com.woocommerce.android.cardreader.config.CardReaderConfigForIT
import com.woocommerce.android.cardreader.config.CardReaderConfigForLU
import com.woocommerce.android.cardreader.config.CardReaderConfigForNL
import com.woocommerce.android.cardreader.config.CardReaderConfigForNZ
import com.woocommerce.android.cardreader.config.CardReaderConfigForPT
import com.woocommerce.android.cardreader.config.CardReaderConfigForSG
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.util.FeatureFlag
import com.woocommerce.android.util.FeatureFlagRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CardReaderCountryConfigProviderTest {
    private val cardReaderConfigFactory: CardReaderConfigFactory = CardReaderConfigFactory()
    private val featureFlagRepository: FeatureFlagRepository = mock {
        on { isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION) }.thenReturn(false)
        on { isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED) }.thenReturn(false)
    }

    private val sut = CardReaderCountryConfigProvider(
        cardReaderConfigFactory,
        featureFlagRepository,
    )

    // --- Base countries: unaffected by flags ---

    @Test
    fun `given CA country code, when config provide, then Canada returned`() {
        assertThat(sut.provideCountryConfigFor("CA"))
            .isInstanceOf(CardReaderConfigForCanada::class.java)
    }

    @Test
    fun `given US country code, when config provide, then USA returned`() {
        assertThat(sut.provideCountryConfigFor("US"))
            .isInstanceOf(CardReaderConfigForUSA::class.java)
    }

    @Test
    fun `given GB country code, when config provide, then GB config returned`() {
        assertThat(sut.provideCountryConfigFor("GB"))
            .isInstanceOf(CardReaderConfigForGB::class.java)
    }

    @Test
    fun `given RU country code, when config provide, then unsupported config returned`() {
        assertThat(sut.provideCountryConfigFor("RU"))
            .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
    }

    // --- Australia stays unsupported with both flags on ---

    @Test
    fun `given AU country code with both expansion flags enabled, when config provide, then unsupported returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)

        assertThat(sut.provideCountryConfigFor("AU"))
            .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
    }

    // --- Primary expansion group: gated by IPP_COUNTRY_EXPANSION ---

    @Test
    fun `given primary expansion country and primary flag on, when config provide, then per-country config returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)

        assertThat(sut.provideCountryConfigFor("FR")).isInstanceOf(CardReaderConfigForFR::class.java)
        assertThat(sut.provideCountryConfigFor("DE")).isInstanceOf(CardReaderConfigForDE::class.java)
        assertThat(sut.provideCountryConfigFor("IE")).isInstanceOf(CardReaderConfigForIE::class.java)
        assertThat(sut.provideCountryConfigFor("NL")).isInstanceOf(CardReaderConfigForNL::class.java)
        assertThat(sut.provideCountryConfigFor("SG")).isInstanceOf(CardReaderConfigForSG::class.java)
        assertThat(sut.provideCountryConfigFor("NZ")).isInstanceOf(CardReaderConfigForNZ::class.java)
    }

    @Test
    fun `given primary expansion country and primary flag off, when config provide, then unsupported returned`() {
        listOf("FR", "DE", "IE", "NL", "SG", "NZ").forEach { code ->
            assertThat(sut.provideCountryConfigFor(code))
                .`as`("Expected $code to be unsupported when primary flag is off")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
    }

    @Test
    fun `given primary expansion country with only EU extended flag on, when config provide, then unsupported returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)

        listOf("FR", "DE", "IE", "NL", "SG", "NZ").forEach { code ->
            assertThat(sut.provideCountryConfigFor(code))
                .`as`("Expected $code to be unsupported when only EU extended flag is on")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
    }

    // --- EU extended group: gated by IPP_COUNTRY_EXPANSION_EU_EXTENDED ---

    @Test
    fun `given EU extended country and EU extended flag on, when config provide, then per-country config returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION_EU_EXTENDED)).thenReturn(true)

        assertThat(sut.provideCountryConfigFor("AT")).isInstanceOf(CardReaderConfigForAT::class.java)
        assertThat(sut.provideCountryConfigFor("BE")).isInstanceOf(CardReaderConfigForBE::class.java)
        assertThat(sut.provideCountryConfigFor("FI")).isInstanceOf(CardReaderConfigForFI::class.java)
        assertThat(sut.provideCountryConfigFor("IT")).isInstanceOf(CardReaderConfigForIT::class.java)
        assertThat(sut.provideCountryConfigFor("LU")).isInstanceOf(CardReaderConfigForLU::class.java)
        assertThat(sut.provideCountryConfigFor("PT")).isInstanceOf(CardReaderConfigForPT::class.java)
        assertThat(sut.provideCountryConfigFor("ES")).isInstanceOf(CardReaderConfigForES::class.java)
    }

    @Test
    fun `given EU extended country and EU extended flag off, when config provide, then unsupported returned`() {
        listOf("AT", "BE", "FI", "IT", "LU", "PT", "ES").forEach { code ->
            assertThat(sut.provideCountryConfigFor(code))
                .`as`("Expected $code to be unsupported when EU extended flag is off")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
    }

    @Test
    fun `given EU extended country with only primary flag on, when config provide, then unsupported returned`() {
        whenever(featureFlagRepository.isEnabled(FeatureFlag.IPP_COUNTRY_EXPANSION)).thenReturn(true)

        listOf("AT", "BE", "FI", "IT", "LU", "PT", "ES").forEach { code ->
            assertThat(sut.provideCountryConfigFor(code))
                .`as`("Expected $code to be unsupported when only primary flag is on")
                .isInstanceOf(CardReaderConfigForUnsupportedCountry::class.java)
        }
    }
}
