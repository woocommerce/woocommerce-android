package com.woocommerce.android.ui.woopos.cardpayment

import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForGB
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosIsCardPaymentEnabledForCountryTest {

    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val site = SiteModel().apply { id = 1 }
    private val selectedSite: SelectedSite = mock { on { getOrNull() } doReturn site }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val countryConfigProvider: CardReaderCountryConfigProvider = mock()

    private val sut = WooPosIsCardPaymentEnabledForCountry(
        selectedSite = selectedSite,
        wooCommerceStore = wooCommerceStore,
        countryConfigProvider = countryConfigProvider,
    )

    @Test
    fun `given POS card-payment enabled country, when invoke, then returns true`() = runTest {
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("GB")
        whenever(countryConfigProvider.provideCountryConfigFor("GB")).thenReturn(CardReaderConfigForGB)

        assertThat(sut()).isTrue
    }

    @Test
    fun `given CA, when invoke, then returns true`() = runTest {
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("CA")
        whenever(countryConfigProvider.provideCountryConfigFor("CA")).thenReturn(CardReaderConfigForCanada)

        assertThat(sut()).isTrue()
    }

    @Test
    fun `given unsupported country, when invoke, then returns false`() = runTest {
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn("JP")
        whenever(countryConfigProvider.provideCountryConfigFor("JP"))
            .thenReturn(CardReaderConfigForUnsupportedCountry)

        assertThat(sut()).isFalse
    }

    @Test
    fun `given no selected site, when invoke, then returns false without consulting config`() = runTest {
        whenever(selectedSite.getOrNull()).thenReturn(null)

        assertThat(sut()).isFalse
    }

    @Test
    fun `given selected site without country code, when invoke, then returns false`() = runTest {
        whenever(wooCommerceStore.getStoreCountryCode(site)).thenReturn(null)

        assertThat(sut()).isFalse
    }
}
