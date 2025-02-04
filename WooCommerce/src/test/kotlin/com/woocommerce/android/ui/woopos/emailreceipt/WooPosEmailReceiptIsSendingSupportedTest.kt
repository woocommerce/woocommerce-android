package com.woocommerce.android.ui.woopos.emailreceipt

import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class WooPosEmailReceiptIsSendingSupportedTest {
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()

    private val receiptSendingAvailable = WooPosEmailReceiptIsSendingSupported(
        getWooCoreVersion = getWooCoreVersion
    )

    @Test
    fun `given wooCoreVersion null, when invoked, then return false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isFalse
    }

    @Test
    fun `given wooCoreVersion less than required, when invoked, then return false`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.4.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isFalse
    }

    @Test
    fun `given wooCoreVersion equal to required, when invoked, then return true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.5.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isTrue
    }

    @Test
    fun `given wooCoreVersion greater than required, when invoked, then return true`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.6.0")

        // WHEN
        val result = receiptSendingAvailable()

        // THEN
        assertThat(result).isTrue
    }
}
