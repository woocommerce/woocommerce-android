package com.woocommerce.android.ui.woopos.home.totals.payment.receipt

import com.woocommerce.android.ui.woopos.featureflags.WooPosIsReceiptsEnabled
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosIsReceiptSendingAvailableTest {
    private val isReceiptsEnabled: WooPosIsReceiptsEnabled = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()

    private val receiptSendingAvailable = WooPosTotalsPaymentReceiptIsSendingAvailable(
        isReceiptsEnabled = isReceiptsEnabled,
        getWooCoreVersion = getWooCoreVersion
    )

    @Test
    fun `given receipts disabled, when invoked, then return false`() = runTest {
        // GIVEN
        whenever(isReceiptsEnabled()).thenReturn(false)

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isFalse
    }

    @Test
    fun `given receipts enabled and wooCoreVersion null, when invoked, then return false`() = runTest {
        // GIVEN
        whenever(isReceiptsEnabled()).thenReturn(true)
        whenever(getWooCoreVersion()).thenReturn(null)

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isFalse
    }

    @Test
    fun `given receipts enabled and wooCoreVersion less than required, when invoked, then return false`() = runTest {
        // GIVEN
        whenever(isReceiptsEnabled()).thenReturn(true)
        whenever(getWooCoreVersion()).thenReturn("9.4.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isFalse
    }

    @Test
    fun `given receipts enabled and wooCoreVersion equal to required, when invoked, then return true`() = runTest {
        // GIVEN
        whenever(isReceiptsEnabled()).thenReturn(true)
        whenever(getWooCoreVersion()).thenReturn("9.5.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isTrue
    }

    @Test
    fun `given receipts enabled and wooCoreVersion greater than required, when invoked, then return true`() = runTest {
        // GIVEN
        whenever(isReceiptsEnabled()).thenReturn(true)
        whenever(getWooCoreVersion()).thenReturn("9.6.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isTrue
    }
}
