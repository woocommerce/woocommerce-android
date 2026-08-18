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
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
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
        ComputedRefundLineItem.quantityBased(lineItemId = 1L, quantity = 2),
        ComputedRefundLineItem.amountBased(lineItemId = 9L, refundTotal = "5.00".toBigDecimal()),
    )

    @Test
    fun `when createComputedRefund, then v3 refunds endpoint is called with computed body`() = runTest {
        // GIVEN
        val response = refundResponse()
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Success(response, emptyList()))

        // WHEN
        val result = sut.createComputedRefund(
            site = site,
            orderId = orderId,
            reason = "reason",
            apiRefund = true,
            apiRestock = true,
            lineItems = lineItems,
        )

        // THEN — the server computes the totals; api_refund/api_restock are always sent explicitly
        // because the v3 endpoint defaults both to true when omitted.
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.orders.id(orderId).refunds.pathV3),
            clazz = eq(RefundResponse::class.java),
            body = bodyCaptor.capture(),
        )
        val body = bodyCaptor.firstValue
        assertThat(body["compute_totals"]).isEqualTo(true)
        assertThat(body["reason"]).isEqualTo("reason")
        assertThat(body["api_refund"]).isEqualTo("true")
        assertThat(body["api_restock"]).isEqualTo("true")
        assertThat(body["line_items"]).isEqualTo(lineItems)
        assertThat(body).doesNotContainKey("amount")
        assertThat(body).doesNotContainKey("order_id")
        assertThat(result.result).isEqualTo(response)
    }

    @Test
    fun `given api flags are false, when createComputedRefund, then they are sent as false`() = runTest {
        // GIVEN
        whenever(
            wooNetwork.executePostGsonRequest(
                site = any(),
                path = any(),
                clazz = eq(RefundResponse::class.java),
                body = any(),
            )
        ).thenReturn(WPAPIResponse.Success(refundResponse(), emptyList()))

        // WHEN
        sut.createComputedRefund(
            site = site,
            orderId = orderId,
            reason = "",
            apiRefund = false,
            apiRestock = false,
            lineItems = lineItems,
        )

        // THEN — no total is ever sent, whatever the flags are.
        val bodyCaptor = argumentCaptor<Map<String, Any>>()
        verify(wooNetwork).executePostGsonRequest(
            site = eq(site),
            path = eq(WOOCOMMERCE.orders.id(orderId).refunds.pathV3),
            clazz = eq(RefundResponse::class.java),
            body = bodyCaptor.capture(),
        )
        val body = bodyCaptor.firstValue
        assertThat(body).doesNotContainKey("amount")
        assertThat(body["api_refund"]).isEqualTo("false")
        assertThat(body["api_restock"]).isEqualTo("false")
    }

    @Test
    fun `given route not registered, when createComputedRefund, then API_NOT_FOUND is returned`() = runTest {
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
        val result = sut.createComputedRefund(
            site = site,
            orderId = orderId,
            reason = "",
            apiRefund = false,
            apiRestock = true,
            lineItems = lineItems,
        )

        // THEN
        assertThat(result.error?.type).isEqualTo(WooErrorType.API_NOT_FOUND)
    }

    private fun refundResponse() = RefundResponse(
        refundId = 55L,
        dateCreated = null,
        amount = "110.00",
        reason = "reason",
        refundedPayment = true,
        items = null,
        shippingLineItems = null,
        feeLineItems = null,
    )
}
