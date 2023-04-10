package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.config.CardReaderConfigForCanada
import com.woocommerce.android.cardreader.config.CardReaderConfigForUSA
import com.woocommerce.android.cardreader.config.CardReaderConfigForUnsupportedCountry
import com.woocommerce.android.ui.payments.cardreader.CardReaderCountryConfigProvider
import com.woocommerce.android.util.DeviceFeatures
import com.woocommerce.android.util.SystemVersionUtilsWrapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class IsTapToPayAvailableTest {
    private val systemVersionUtilsWrapper = mock<SystemVersionUtilsWrapper> {
        on { isAtLeastP() }.thenReturn(true)
    }
    private val appPrefs: AppPrefs = mock()
    private val cardReaderCountryConfigProvider: CardReaderCountryConfigProvider = mock {
        on { provideCountryConfigFor("US") }.thenReturn(CardReaderConfigForUSA)
        on { provideCountryConfigFor("CA") }.thenReturn(CardReaderConfigForCanada)
        on { provideCountryConfigFor("RU") }.thenReturn(CardReaderConfigForUnsupportedCountry)
    }

    @Test
    fun `given device has no NFC and ttp enabled, when invoking, then nfc disabled returned`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(false)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(true)

        val result = IsTapToPayAvailable(
            appPrefs,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("US")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.NotAvailable.NfcNotAvailable)
    }

    @Test
    fun `given device has no Google Play Services and ttp enabled, when invoking, then GPS not available`() {
        val deviceFeatures = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(false)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(true)

        val result = IsTapToPayAvailable(
            appPrefs,
            deviceFeatures,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("US")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.NotAvailable.GooglePlayServicesNotAvailable)
    }

    @Test
    fun `given device has os less than Android 9 and ttp enabled, when invoking, then system is not supported returned`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(false)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(true)

        val result = IsTapToPayAvailable(
            appPrefs,
            context,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("US")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.NotAvailable.SystemVersionNotSupported)
    }

    @Test
    fun `given country other than US and ttp enabled, when invoking, then country is not supported returned`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(true)

        val result = IsTapToPayAvailable(
            appPrefs,
            context,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("CA")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.NotAvailable.CountryNotSupported)
    }

    @Test
    fun `given tap to pay feature flag is not enabled, when invoking, then tpp is disabled returned`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(false)

        val result = IsTapToPayAvailable(
            appPrefs,
            context,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("US")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.NotAvailable.TapToPayDisabled)
    }

    @Test
    fun `given device satisfies all the requirements and ttp enabled, when invoking, then tpp available returned`() {
        val context = mock<DeviceFeatures> {
            whenever(it.isNFCAvailable()).thenReturn(true)
            whenever(it.isGooglePlayServicesAvailable()).thenReturn(true)
        }
        whenever(systemVersionUtilsWrapper.isAtLeastP()).thenReturn(true)
        whenever(appPrefs.isTapToPayEnabled).thenReturn(true)

        val result = IsTapToPayAvailable(
            appPrefs,
            context,
            systemVersionUtilsWrapper,
            cardReaderCountryConfigProvider
        ).invoke("US")

        assertThat(result).isEqualTo(IsTapToPayAvailable.Result.Available)
    }
}
