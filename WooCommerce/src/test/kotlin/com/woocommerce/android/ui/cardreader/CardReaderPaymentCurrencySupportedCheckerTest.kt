package com.woocommerce.android.ui.cardreader

import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.internal.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.payment.CardReaderPaymentCurrencySupportedChecker
import com.woocommerce.android.viewmodel.BaseUnitTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CardReaderPaymentCurrencySupportedCheckerTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock()
    private val wooStore: WooCommerceStore = mock()
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock()

    private val site = SiteModel()
    private val countryCode = "US"

    private val cardReaderPaymentCurrencySupportedChecker = CardReaderPaymentCurrencySupportedChecker(
        coroutinesTestRule.testDispatchers,
        wooStore,
        selectedSite,
        cardReaderCountryConfigProvider,
    )

    @Before
    fun setUp() {
        whenever(selectedSite.get()).thenReturn(site)
        whenever(wooStore.getStoreCountryCode(site)).thenReturn(countryCode)
    }

    @Test
    fun `given usd currency, when store location in USA, then isCurrencySupported returns true`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
            whenever(
                cardReaderCountryConfigProvider.provideCountryConfigFor("US")
            ).thenReturn(CardReaderConfigForUSA)

            val isCurrencySupported = cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("USD")

            assertTrue(isCurrencySupported)
        }

    @Test
    fun `given usd currency, when store location in Canada, then isCurrencySupported returns false`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
            whenever(
                cardReaderCountryConfigProvider.provideCountryConfigFor("CA")
            ).thenReturn(CardReaderConfigForCanada)

            val isCurrencySupported = cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("USD")

            assertFalse(isCurrencySupported)
        }

    @Test
    fun `given cad currency, when store location in Canada, then isCurrencySupported returns true`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("CA")
            whenever(
                cardReaderCountryConfigProvider.provideCountryConfigFor("CA")
            ).thenReturn(CardReaderConfigForCanada)

            val isCurrencySupported = cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("CAD")

            assertTrue(isCurrencySupported)
        }

    @Test
    fun `given cad currency, when store location in USA, then isCurrencySupported returns false`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("US")
            whenever(
                cardReaderCountryConfigProvider.provideCountryConfigFor("US")
            ).thenReturn(CardReaderConfigForUSA)

            val isCurrencySupported = cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("CAD")

            assertFalse(isCurrencySupported)
        }

    @Test
    fun `given usd currency, when store location in unsupported country, then isCurrencySupported returns false`() =
        testBlocking {
            whenever(wooStore.getStoreCountryCode(site)).thenReturn("IN")
            whenever(
                cardReaderCountryConfigProvider.provideCountryConfigFor("IN")
            ).thenReturn(CardReaderConfigForUnsupportedCountry)

            val isCurrencySupported = cardReaderPaymentCurrencySupportedChecker.isCurrencySupported("USD")

            assertFalse(isCurrencySupported)
        }
}
