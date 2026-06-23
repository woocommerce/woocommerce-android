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
import org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds.RefundRestClient.RefundResponse

@OptIn(ExperimentalCoroutinesApi::class)
class RefundRestClientTest {
    private val wooNetwork: WooNetwork = mock()
    private val site = SiteModel()
    private val sut = RefundRestClient(wooNetwork)

    private val orderId = 123L
    private val lineItems = listOf(
        RefundV4LineItem(lineItemId = 1L, quantity = 2),
        RefundV4LineItem(lineItemId = 9L, refundTotal = "5.00".toBigDecimal()),
    )

    @Test
    fun `when createSimplifiedRefund, then v4 refunds endpoint is called with simplified body`() = runTest {
        // GIVEN
        val response = RefundResponse(
            refundId = 55L,
            dateCreated = null,
            amount = "110.00",
            reason = "reason",
            refundedPayment = true,
            items = null,
            shippingLineItems = null,
            feeLineItems = null,
        )
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Success(response, emptyList()))

        // WHEN
        val result = sut.createSimplifiedRefund(
            site = site,
            orderId = orderId,
            reason = "reason",
            automaticRefund = true,
            items = lineItems,
        )

        // THEN — only describes what to refund: no client-calculated amount/refund_total at top level.
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.refunds.pathV4),
            clazz = eq(RefundResponse::class.java),
            body = bodyCaptor.capture(),
        )
        val body = bodyCaptor.firstValue
        assertThat(body["order_id"]).isEqualTo(orderId)
        assertThat(body["reason"]).isEqualTo("reason")
        assertThat(body["api_refund"]).isEqualTo("true")
        assertThat(body["line_items"]).isEqualTo(lineItems)
        assertThat(body).doesNotContainKey("amount")
        assertThat(result.result).isEqualTo(response)
    }

    @Test
    fun `given route not registered, when createSimplifiedRefund, then API_NOT_FOUND is returned`() = runTest {
        // GIVEN
        val networkError = WPAPINetworkError(
            BaseNetworkError(GenericErrorType.NOT_FOUND, "No route"),
            errorCode = "rest_no_route",
        )
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Error(networkError))

        // WHEN
        val result = sut.createSimplifiedRefund(
            site = site,
            orderId = orderId,
            reason = "",
            automaticRefund = false,
            items = lineItems,
        )

        // THEN
        assertThat(result.error?.type).isEqualTo(WooErrorType.API_NOT_FOUND)
    }
}
