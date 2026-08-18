package org.wordpress.android.fluxc.network.rest.wpcom.wc.refunds

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.refunds.ComputedRefundLineItem
import org.wordpress.android.fluxc.model.refunds.RefundRequestItem
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundFeeLine
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundShippingLine
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.extensions.filterNotNull
import org.wordpress.android.fluxc.utils.toWooPayload
import java.math.BigDecimal
import javax.inject.Inject

class RefundRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun createRefundByAmount(
        site: SiteModel,
        orderId: Long,
        amount: String,
        reason: String,
        automaticRefund: Boolean
    ): WooPayload<RefundResponse> {
        val body = mapOf(
                "amount" to amount,
                "reason" to reason,
                "api_refund" to automaticRefund.toString()
        )
        return createRefund(site, orderId, body)
    }

    @Suppress("LongParameterList")
    suspend fun createRefundByItems(
        site: SiteModel,
        orderId: Long,
        amount: BigDecimal?,
        reason: String,
        automaticRefund: Boolean,
        items: List<RefundRequestItem>,
        restockItems: Boolean
    ): WooPayload<RefundResponse> {
        val body = mapOf(
                "reason" to reason,
                "amount" to amount?.toString(),
                "api_refund" to automaticRefund.toString(),
                "line_items" to items,
                "restock_items" to restockItems
        ).filterNotNull()

        return createRefund(site, orderId, body)
    }

    /**
     * Creates a server-computed refund (`POST /wc/v3/orders/<order_id>/refunds` with
     * `compute_totals=true`).
     *
     * The client sends only what to refund (line item IDs + quantities, or amounts for fee/shipping
     * lines) and the server computes every monetary value. No order-level total is sent. The
     * endpoint accepts one as an override, but this client does not expose it: sending a total
     * would make the caller, not the order, the authority on what the refund is worth.
     *
     * [apiRefund] and [apiRestock] are always sent explicitly: the v3 endpoint defaults both to
     * `true` when omitted, and the caller must stay in control of gateway refunds and restocking.
     *
     * On stores whose refund endpoint does not support `compute_totals`, the unknown param is
     * silently dropped and the request is handled by the classic create, where a quantity-only
     * body carries no monetary value to sum: the store books a zero-amount refund, restocks the
     * items, and answers 201. Callers must therefore only use this method against a store known
     * to support `compute_totals` (see the POS refund flow resolver and availability cache).
     */
    suspend fun createComputedRefund(
        site: SiteModel,
        orderId: Long,
        reason: String,
        apiRefund: Boolean,
        apiRestock: Boolean,
        lineItems: List<ComputedRefundLineItem>,
    ): WooPayload<RefundResponse> {
        val body = mapOf(
            "compute_totals" to true,
            "reason" to reason,
            "api_refund" to apiRefund.toString(),
            "api_restock" to apiRestock.toString(),
            "line_items" to lineItems,
        )

        return createRefund(site, orderId, body)
    }

    private suspend fun createRefund(
        site: SiteModel,
        orderId: Long,
        body: Map<String, Any>
    ): WooPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3
        val response = wooNetwork.executePostGsonRequest(
                site = site,
                path = url,
                body = body,
                clazz = RefundResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun fetchRefund(
        site: SiteModel,
        orderId: Long,
        refundId: Long
    ): WooPayload<RefundResponse> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.refund(refundId).pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = RefundResponse::class.java
        )
        return response.toWooPayload()
    }

    suspend fun fetchAllRefunds(
        site: SiteModel,
        orderId: Long,
        page: Int,
        pageSize: Int
    ): WooPayload<Array<RefundResponse>> {
        val url = WOOCOMMERCE.orders.id(orderId).refunds.pathV3

        val response = wooNetwork.executeGetGsonRequest(
            site = site,
            path = url,
            clazz = Array<RefundResponse>::class.java,
            params = mapOf(
                "page" to page.toString(),
                "per_page" to pageSize.toString()
            )
        )
        return response.toWooPayload()
    }

    data class RefundResponse(
        @SerializedName("id") val refundId: Long,
        @SerializedName("date_created") val dateCreated: String?,
        @SerializedName("amount") val amount: String?,
        @SerializedName("reason") val reason: String?,
        @SerializedName("refunded_payment") val refundedPayment: Boolean?,
        @SerializedName("line_items") val items: List<LineItem>?,
        @SerializedName("shipping_lines") val shippingLineItems: List<WCRefundShippingLine>?,
        @SerializedName("fee_lines") val feeLineItems: List<WCRefundFeeLine>?
    )
}
