package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore

class TapToPayAvailabilityStatusTest {
    private val systemVersionUtilsWrapper = mock<SystemVersionUtilsWrapper> {
        on { isAtLeastQ() }.thenReturn(true)
    }
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock {
        on { provideCountryConfigFor("US") }.thenReturn(CardReaderConfigForUSA)
        on { provideCountryConfigFor("CA") }.thenReturn(CardReaderConfigForCanada)
        on { provideCountryConfigFor("RU") }.thenReturn(CardReaderConfigForUnsupportedCountry)
    }
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val wooStore: WooCommerceStore = mock {
        on { getStoreCountryCode(siteModel) }.thenReturn("US")
    }

    @Test
    fun `given device has no NFC, when invoking, then nfc disabled returned`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(false)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastQ()).thenReturn(true)

        val result = TapToPayAvailabilityStatus(
            selectedSite,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider,
            wooStore
        ).invoke()

        assertThat(result).isEqualTo(TapToPayAvailabilityStatus.Result.NotAvailable.NfcNotAvailable)
    }

    @Test
    fun `given device has no Google Play Services, when invoking, then GPS not available`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(false)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastQ()).thenReturn(true)

        val result = TapToPayAvailabilityStatus(
            selectedSite,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider,
            wooStore
        ).invoke()

        assertThat(result).isEqualTo(TapToPayAvailabilityStatus.Result.NotAvailable.GooglePlayServicesNotAvailable)
    }

    @Test
    fun `given device has os less than Android 9, when invoking, then system is not supported returned`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastQ()).thenReturn(false)

        val result = TapToPayAvailabilityStatus(
            selectedSite,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider,
            wooStore
        ).invoke()

        assertThat(result).isEqualTo(TapToPayAvailabilityStatus.Result.NotAvailable.SystemVersionNotSupported)
    }

    @Test
    fun `given country other than US, when invoking, then country is not supported returned`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastQ()).thenReturn(true)
        whenever(wooStore.getStoreCountryCode(siteModel)).thenReturn("RU")

        val result = TapToPayAvailabilityStatus(
            selectedSite,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider,
            wooStore
        ).invoke()

        assertThat(result).isEqualTo(TapToPayAvailabilityStatus.Result.NotAvailable.CountryNotSupported)
    }

    @Test
    fun `given device satisfies all the requirements, when invoking, then tpp available returned`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastQ()).thenReturn(true)

        val result = TapToPayAvailabilityStatus(
            selectedSite,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider,
            wooStore
        ).invoke()

        assertThat(result).isEqualTo(TapToPayAvailabilityStatus.Result.Available)
    }
}
