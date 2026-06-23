package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPINetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundPreviewRestClient.RefundPreviewResponse

@OptIn(ExperimentalCoroutinesApi::class)
class RefundPreviewRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val site = SiteModel()
    private val sut = RefundPreviewRestClient(wooNetwork)

    private val orderId = 123L
    private val lineItems = listOf(
        RefundV4LineItem(lineItemId = 1L, quantity = 2),
        RefundV4LineItem(lineItemId = 9L, refundTotal = "5.00".toBigDecimal()),
    )

    @Test
    fun `when previewRefund, then v4 preview endpoint is called with order id and line items`() = runTest {
        // GIVEN
        val response = RefundPreviewResponse(
            breakdown = null,
            subtotal = "100.00",
            tax = "10.00",
            total = "110.00",
            maxRefundable = "200.00",
        )
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundPreviewResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Success(response, emptyList()))

        // WHEN
        val result = sut.previewRefund(site, orderId, lineItems)

        // THEN
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.refunds.preview.pathV4),
            clazz = eq(RefundPreviewResponse::class.java),
            body = bodyCaptor.capture(),
        )
        assertThat(bodyCaptor.firstValue["order_id"]).isEqualTo(orderId)
        assertThat(bodyCaptor.firstValue["line_items"]).isEqualTo(lineItems)
        assertThat(result.result).isEqualTo(response)
        assertThat(result.isError).isFalse()
    }

    @Test
    fun `given route not registered, when previewRefund, then API_NOT_FOUND is returned`() = runTest {
        // GIVEN
        val networkError = WPAPINetworkError(
            BaseNetworkError(GenericErrorType.NOT_FOUND, "No route"),
            errorCode = "rest_no_route",
        )
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundPreviewResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Error(networkError))

        // WHEN
        val result = sut.previewRefund(site, orderId, lineItems)

        // THEN
        assertThat(result.error?.type).isEqualTo(WooErrorType.API_NOT_FOUND)
        assertThat(result.error?.apiErrorCode).isEqualTo("rest_no_route")
    }
}
