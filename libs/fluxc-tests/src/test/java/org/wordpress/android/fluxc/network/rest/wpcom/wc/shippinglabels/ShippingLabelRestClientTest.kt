package org.wordpress.android.fluxc.network.rest.wpcom.wc.shippinglabels

import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelPackageData
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingPackageCustoms
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork

class ShippingLabelRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val restClient = ShippingLabelRestClient(wooNetwork)

    @Test
    fun `when purchase shipping label then pass correct source param to network client`() {
        runBlocking {
            // GIVEN
            whenever(
                wooNetwork.executePostGsonRequest(
                    any(),
                    any(),
                    eq(ShippingLabelStatusApiResponse::class.java),
                    any()
                )
            ).thenReturn(
                WPAPIResponse.Success(mock())
            )
            // WHEN
            val siteModel = SiteModel()
            val orderId = 1L
            val origin: ShippingLabelAddress = mock()
            val destination: ShippingLabelAddress = mock()
            val packagesData: List<WCShippingLabelPackageData> = emptyList()
            val customsData: List<WCShippingPackageCustoms>? = null
            val emailReceipts = false
            restClient.purchaseShippingLabels(
                siteModel,
                orderId,
                origin,
                destination,
                packagesData,
                customsData,
                emailReceipts
            )

            // THEN
            val bodyCaptor = argumentCaptor<Map<String, Any>>()
            verify(wooNetwork).executePostGsonRequest(
                site = eq(siteModel),
                path = eq("/wc/v1/connect/label/1/"),
                clazz = eq(ShippingLabelStatusApiResponse::class.java),
                body = bodyCaptor.capture()
            )
            assertThat(bodyCaptor.firstValue["source"]).isEqualTo("wc-android")
        }
    }
}
