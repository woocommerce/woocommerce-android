package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WooCommerceStore

class WooPosSettingsReceiptRepositoryTest {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val wooCommerceStore: WooCommerceStore = mock()
    private val getWooCoreVersion: GetWooCorePluginCachedVersion = mock()

    private val repository = WooPosSettingsReceiptRepository(
        selectedSite,
        wooCommerceStore,
        getWooCoreVersion
    )

    @Test
    fun `given WooCommerce version is less than 10, when getReceiptInfo, then returns NotAvailable`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("9.9.9")

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isEqualTo(WooPosReceiptDataResult.NotAvailable)
    }

    @Test
    fun `given WooCommerce version is null, when getReceiptInfo, then returns NotAvailable`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn(null)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isEqualTo(WooPosReceiptDataResult.NotAvailable)
    }

    @Test
    fun `given WooCommerce version is 10 or higher and API succeeds, when getReceiptInfo, then returns Success with receipt info`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        val settingsMap = mapOf(
            "woocommerce_pos_store_name" to "Test Store",
            "woocommerce_pos_store_address" to "123 Test Street",
            "woocommerce_pos_store_phone" to "+1234567890",
            "woocommerce_pos_refund_returns_policy" to "30 day returns"
        )
        val apiResult = WooResult(settingsMap)
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenReturn(apiResult)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isInstanceOf(WooPosReceiptDataResult.Success::class.java)
        val successData = result as WooPosReceiptDataResult.Success
        assertThat(successData.receiptInfo.storeName).isEqualTo("Test Store")
        assertThat(successData.receiptInfo.address).isEqualTo("123 Test Street")
        assertThat(successData.receiptInfo.phone).isEqualTo("+1234567890")
        assertThat(successData.receiptInfo.refundPolicy).isEqualTo("30 day returns")
    }

    @Test
    fun `given WooCommerce version is 10 or higher and API returns empty settings, when getReceiptInfo, then returns Success with empty receipt info`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.1.0")
        val apiResult = WooResult(emptyMap<String, String>())
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenReturn(apiResult)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isInstanceOf(WooPosReceiptDataResult.Success::class.java)
        val successData = result as WooPosReceiptDataResult.Success
        assertThat(successData.receiptInfo.storeName).isEqualTo("")
        assertThat(successData.receiptInfo.address).isEqualTo("")
        assertThat(successData.receiptInfo.phone).isEqualTo("")
        assertThat(successData.receiptInfo.refundPolicy).isEqualTo("")
    }

    @Test
    fun `given WooCommerce version is 10 or higher and API returns error, when getReceiptInfo, then returns Error`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        val errorResult = WooResult<Map<String, String>>(
            WooError(WooErrorType.API_ERROR, GenericErrorType.NETWORK_ERROR, "Network error")
        )
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenReturn(errorResult)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isEqualTo(WooPosReceiptDataResult.Error)
    }

    @Test
    fun `given WooCommerce version is 10 or higher and API throws exception, when getReceiptInfo, then returns Error`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenThrow(RuntimeException("API failure"))

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isEqualTo(WooPosReceiptDataResult.Error)
    }

    @Test
    fun `given WooCommerce version is 11, when getReceiptInfo, then returns Success`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("11.0.0")
        val settingsMap = mapOf("woocommerce_pos_store_name" to "Future Store")
        val apiResult = WooResult(settingsMap)
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenReturn(apiResult)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isInstanceOf(WooPosReceiptDataResult.Success::class.java)
        val successData = result as WooPosReceiptDataResult.Success
        assertThat(successData.receiptInfo.storeName).isEqualTo("Future Store")
    }

    @Test
    fun `given WooCommerce version is exactly 10_0_0, when getReceiptInfo, then returns Success`() = runTest {
        // GIVEN
        whenever(getWooCoreVersion()).thenReturn("10.0.0")
        val settingsMap = mapOf("woocommerce_pos_store_name" to "Exact Version Store")
        val apiResult = WooResult(settingsMap)
        whenever(wooCommerceStore.fetchPosSettings(siteModel)).thenReturn(apiResult)

        // WHEN
        val result = repository.getReceiptInfo()

        // THEN
        assertThat(result).isInstanceOf(WooPosReceiptDataResult.Success::class.java)
    }
}
