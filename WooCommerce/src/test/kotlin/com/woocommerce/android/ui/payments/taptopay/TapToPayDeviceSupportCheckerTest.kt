package com.woocommerce.android.ui.payments.taptopay

import com.woocommerce.android.AppPrefs
import com.woocommerce.android.cardreader.CardReaderManager
import com.woocommerce.android.cardreader.connection.TapToPaySupportResult
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class TapToPayDeviceSupportCheckerTest {
    private val cardReaderManager: CardReaderManager = mock()
    private val appPrefs: AppPrefs = mock()
    private val checker = TapToPayDeviceSupportChecker(cardReaderManager, appPrefs)

    @Test
    fun `given Stripe reports supported, when isSupported, then true returned`() {
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.Supported)

        val result = checker.isSupported()

        assertThat(result).isTrue
    }

    @Test
    fun `given Stripe reports not supported, when isSupported, then false returned`() {
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.NotSupported("no TEE"))

        val result = checker.isSupported()

        assertThat(result).isFalse
    }

    @Test
    fun `given Terminal not initialized, when isSupported, then null returned`() {
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.TerminalNotInitialized)

        val result = checker.isSupported()

        assertThat(result).isNull()
    }

    @Test
    fun `given Stripe answered once, when isSupported called again, then result is cached`() {
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.Supported)

        checker.isSupported()
        checker.isSupported()
        checker.isSupported()

        verify(cardReaderManager).isTapToPaySupportedOnDevice(false)
    }

    @Test
    fun `given simulated reader enabled, when isSupported, then Stripe is asked about the simulated reader`() {
        // GIVEN
        whenever(appPrefs.isSimulatedReaderEnabled).thenReturn(true)
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(true))
            .thenReturn(TapToPaySupportResult.Supported)

        // WHEN
        val result = checker.isSupported()

        // THEN
        assertThat(result).isTrue
        verify(cardReaderManager).isTapToPaySupportedOnDevice(true)
    }

    @Test
    fun `given Terminal not initialized, when isSupported called again, then result is not cached`() {
        whenever(cardReaderManager.isTapToPaySupportedOnDevice(false))
            .thenReturn(TapToPaySupportResult.TerminalNotInitialized)
            .thenReturn(TapToPaySupportResult.Supported)

        checker.isSupported()
        val secondCall = checker.isSupported()

        assertThat(secondCall).isTrue
    }
}
