package com.woocommerce.android.ui.payments.receipt

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.store.WooCommerceStore

@ExperimentalCoroutinesApi
class PaymentReceiptHelperTest {
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val wooCommerceStore: WooCommerceStore = mock()

    private val helper = PaymentReceiptHelper(selectedSite, wooCommerceStore, appPrefsWrapper)

    @Test
    fun `given selected site, when storeReceiptUrl, then url is stored`() {
        // GIVEN
        val site = selectedSite.get()

        // WHEN
        helper.storeReceiptUrl(1, "url")

        // THEN
        verify(appPrefsWrapper).setReceiptUrl(site.id, site.siteId, site.selfHostedSiteId, 1, "url")
    }

    @Test
    fun `given selected site and no saved url, when getReceiptUrl, then null returned`() {
        // GIVEN
        val site = selectedSite.get()
        whenever(appPrefsWrapper.getReceiptUrl(site.id, site.siteId, site.selfHostedSiteId, 1)).thenReturn(null)

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result).isNull()
    }

    @Test
    fun `given selected site and saved url, when getReceiptUrl, then url returned`() {
        // GIVEN
        val site = selectedSite.get()
        whenever(appPrefsWrapper.getReceiptUrl(site.id, site.siteId, site.selfHostedSiteId, 1)).thenReturn("url")

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result).isEqualTo("url")
    }

    @Test
    fun `given null saved plugin version, when isPluginCanSendReceipt, then result is false`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(appPrefsWrapper.getCardReaderPreferredPluginVersion(any(), any(), any(), any())).thenReturn(null)

        // WHEN
        val result = helper.isPluginCanSendReceipt(selectedSite.get())

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given saved plugin version 3-9-9, when isPluginCanSendReceipt, then result is false`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(appPrefsWrapper.getCardReaderPreferredPluginVersion(any(), any(), any(), any()))
            .thenReturn("3.9.9")

        // WHEN
        val result = helper.isPluginCanSendReceipt(selectedSite.get())

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given saved plugin version 4-0-0, when isPluginCanSendReceipt, then result is true`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(appPrefsWrapper.getCardReaderPreferredPluginVersion(any(), any(), any(), any()))
            .thenReturn("4.0.0")

        // WHEN
        val result = helper.isPluginCanSendReceipt(selectedSite.get())

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given saved plugin version 16-3-13, when isPluginCanSendReceipt, then result is true`() {
        // GIVEN
        whenever(appPrefsWrapper.getCardReaderPreferredPlugin(any(), any(), any()))
            .thenReturn(PluginType.WOOCOMMERCE_PAYMENTS)
        whenever(appPrefsWrapper.getCardReaderPreferredPluginVersion(any(), any(), any(), any()))
            .thenReturn("16.3.13")

        // WHEN
        val result = helper.isPluginCanSendReceipt(selectedSite.get())

        // THEN
        assertThat(result).isTrue()
    }
}
