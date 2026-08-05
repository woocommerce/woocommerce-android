package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import com.woocommerce.android.ui.prefs.developer.DeveloperOptionsRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TapToPayDeviceSupportCheckerTest {
    private val cardReaderManager: CardReaderManager = mock()
    private val developerOptionsRepository: DeveloperOptionsRepository = mock()
    private val checker = TapToPayDeviceSupportChecker(cardReaderManager, developerOptionsRepository)

    @Test
    fun `given Stripe reports supported, when checking support, then supported returned`() {
        // GIVEN
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.Supported)

        // WHEN
        val result = checker.checkSupport()

        // THEN
        assertThat(result).isEqualTo(TapToPayDeviceSupport.Supported)
    }

    @Test
    fun `given Stripe reports not supported, when checking support, then not supported returned`() {
        // GIVEN
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.NotSupported("no TEE"))

        // WHEN
        val result = checker.checkSupport()

        // THEN
        assertThat(result).isEqualTo(TapToPayDeviceSupport.NotSupported)
    }

    @Test
    fun `given Terminal not initialized, when checking support, then unknown returned`() {
        // GIVEN
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.TerminalNotInitialized)

        // WHEN
        val result = checker.checkSupport()

        // THEN
        assertThat(result).isEqualTo(TapToPayDeviceSupport.Unknown)
    }

    @Test
    fun `given Stripe answered once, when checking support again, then result is cached`() {
        // GIVEN
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.Supported)

        // WHEN
        checker.checkSupport()
        checker.checkSupport()
        checker.checkSupport()

        // THEN
        verify(cardReaderManager).isTapToPaySupportedOnDevice(false)
    }

    @Test
    fun `given simulated reader enabled, when checking support, then Stripe is asked about the simulated reader`() {
        // GIVEN
        whenever(developerOptionsRepository.isSimulatedCardReaderEnabled()).thenReturn(true)
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(true))
            .thenReturn(TapToPaySupportResult.Supported)

        // WHEN
        val result = checker.checkSupport()

        // THEN
        assertThat(result).isEqualTo(TapToPayDeviceSupport.Supported)
        verify(cardReaderManager).isTapToPaySupportedOnDevice(true)
    }

    @Test
    fun `given Terminal not initialized, when checking support again, then result is not cached`() {
        // GIVEN
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.TerminalNotInitialized)
            .thenReturn(TapToPaySupportResult.Supported)

        // WHEN
        checker.checkSupport()
        val secondCall = checker.checkSupport()

        // THEN
        assertThat(secondCall).isEqualTo(TapToPayDeviceSupport.Supported)
    }
}
