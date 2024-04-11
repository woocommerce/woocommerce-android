package com.woocommerce.android.ui.payments.receipt

import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.payments.cardreader.onboarding.PluginType
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.OrderReceiptResponse
import org.wordpress.android.fluxc.store.WCOrderStore

@ExperimentalCoroutinesApi
class PaymentReceiptHelperTest : BaseUnitTest() {
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(mock())
    }
    private val appPrefsWrapper: AppPrefsWrapper = mock()
    private val getWooVersion: GetWooCorePluginCachedVersion = mock()
    private val orderStore: WCOrderStore = mock()

    private val helper = PaymentReceiptHelper(selectedSite, appPrefsWrapper, orderStore, getWooVersion)

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
    fun `given version 3_9_9 and no saved url, when getReceiptUrl, then failure returned`() = testBlocking {
        // GIVEN
        val site = selectedSite.get()
        whenever(appPrefsWrapper.getReceiptUrl(site.id, site.siteId, site.selfHostedSiteId, 1)).thenReturn("")
        whenever(getWooVersion()).thenReturn("3.9.9")

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result.isFailure).isTrue()
    }

    @Test
    fun `given version 3_9_9 site and saved url, when getReceiptUrl, then url returned`() = testBlocking {
        // GIVEN
        val site = selectedSite.get()
        whenever(appPrefsWrapper.getReceiptUrl(site.id, site.siteId, site.selfHostedSiteId, 1)).thenReturn("url")
        whenever(getWooVersion()).thenReturn("3.9.9")

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result.getOrThrow()).isEqualTo("url")
    }

    @Test
    fun `given version 8_7_0 site and remote call success, when getReceiptUrl, then url returned`() = testBlocking {
        // GIVEN
        val site = selectedSite.get()
        whenever(getWooVersion()).thenReturn("8.7.0")

        whenever(orderStore.fetchOrdersReceipt(site, 1, expirationDays = 2)).thenReturn(
            WooPayload(OrderReceiptResponse("url", "date"))
        )

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result.getOrThrow()).isEqualTo("url")
    }

    @Test
    fun `given version 8_7_0_10 site and saved url, when getReceiptUrl, then url returned`() = testBlocking {
        // GIVEN
        val site = selectedSite.get()
        whenever(getWooVersion()).thenReturn("8.7.0.10")
        whenever(orderStore.fetchOrdersReceipt(site, 1, expirationDays = 2)).thenReturn(
            WooPayload(OrderReceiptResponse("url", "date"))
        )

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result.getOrThrow()).isEqualTo("url")
    }

    @Test
    fun `given version 8_7_0 site and remote call fails, when getReceiptUrl, then failure returned`() = testBlocking {
        // GIVEN
        val site = selectedSite.get()
        whenever(getWooVersion()).thenReturn("8.7.0.10")
        whenever(orderStore.fetchOrdersReceipt(site, 1, expirationDays = 2)).thenReturn(
            WooPayload(
                WooError(
                    type = WooErrorType.API_ERROR,
                    original = BaseRequest.GenericErrorType.NETWORK_ERROR,
                    message = "error"
                )
            )
        )

        // WHEN
        val result = helper.getReceiptUrl(1)

        // THEN
        assertThat(result.isFailure).isTrue()
        assertThat(result.exceptionOrNull()!!.message).isEqualTo("error")
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

    @Test
    fun `given version 8_7_0, when isReceiptAvailable, then true returned`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("8.7.0")

        // WHEN
        val result = helper.isReceiptAvailable(1)

        // THEN
        assertThat(result).isTrue()
    }

    @Test
    fun `given version empty and empty local storage, when isReceiptAvailable, then false returned`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("")
        whenever(appPrefsWrapper.getReceiptUrl(any(), any(), any(), any())).thenReturn("")

        // WHEN
        val result = helper.isReceiptAvailable(1)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version empty string and empty local storage, when isReceiptAvailable, then false returned`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("")
        whenever(appPrefsWrapper.getReceiptUrl(any(), any(), any(), any())).thenReturn("")

        // WHEN
        val result = helper.isReceiptAvailable(1)

        // THEN
        assertThat(result).isFalse()
    }

    @Test
    fun `given version empty 6_3_9 and non empty local storage, when isReceiptAvailable, then true returned`() = testBlocking {
        // GIVEN
        whenever(getWooVersion()).thenReturn("")
        whenever(appPrefsWrapper.getReceiptUrl(any(), any(), any(), any())).thenReturn("url")

        // WHEN
        val result = helper.isReceiptAvailable(1)

        // THEN
        assertThat(result).isTrue()
    }
}
