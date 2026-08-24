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
     * The client sends what to refund, the server computes the money. [amount] is an optional
     * order-level override: below the computed total the store returns 400, above it the store
     * accepts an over-refund up to the order's remaining refundable amount.
     *
     * [apiRefund] and [apiRestock] are sent explicitly because the endpoint defaults both to true.
     *
     * A store without `compute_totals` drops the param and books a zero-amount refund, and
     * restocks when [apiRestock] is true. Only call this on a store known to support it: POS checks
     * the version in `WooPosResolveRefundFlow` and the preview result in
     * `WooPosRefundViewModel.buildSubmissionRequest`.
     */
    @Suppress("LongParameterList")
    suspend fun createComputedRefund(
        site: SiteModel,
        orderId: Long,
        reason: String,
        apiRefund: Boolean,
        apiRestock: Boolean,
        amount: String?,
        lineItems: List<ComputedRefundLineItem>,
    ): WooPayload<RefundResponse> {
        val body = mapOf(
            "compute_totals" to true,
            "reason" to reason,
            "amount" to amount,
            "api_refund" to apiRefund.toString(),
            "api_restock" to apiRestock.toString(),
            "line_items" to lineItems,
        ).filterNotNull()

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
