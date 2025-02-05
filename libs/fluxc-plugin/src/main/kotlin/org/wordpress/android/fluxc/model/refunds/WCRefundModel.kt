package org.wordpress.android.fluxc.model.refunds

import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import java.math.BigDecimal
import java.util.Date

/**
 * Note:
 * For the purpose of creating a refund through API, only a list of `WCRefundItem` is accepted. To be able to refund
 * shipping lines, they will need to be converted into a list of `WCRefundItem` before being sent to the API.
 *
 * On the other hand, for the purpose of fetching a refund through API and persisting the data, product items and
 * shipping lines are listed separately in the API response. To mimic that, here we also add `shippingLineItems` as
 * one of the class properties.
 */
data class WCRefundModel(
    val id: Long,
    val dateCreated: Date,
    val amount: BigDecimal,
    val reason: String?,
    val automaticGatewayRefund: Boolean,
    val items: List<WCRefundItem>,
    val shippingLineItems: List<WCRefundShippingLine>,
    val feeLineItems: List<WCRefundFeeLine>
) {
    data class WCRefundItem(
        val itemId: Long,
        @SerializedName("qty")
        val quantity: Int,
        @SerializedName("refund_total")
        val subtotal: BigDecimal,
        @SerializedName("refund_tax")
        val totalTax: BigDecimal,
        val name: String? = null,
        val productId: Long? = null,
        val variationId: Long? = null,
        val total: BigDecimal? = null,
        val sku: String? = null,
        val price: BigDecimal? = null,
        @SerializedName("meta_data")
        val metaData: List<WCMetaData>? = null
    )

    data class WCRefundShippingLine(
        val id: Long,
        val total: BigDecimal,
        @SerializedName("total_tax")
        val totalTax: BigDecimal,
        @SerializedName("method_id")
        val methodId: String? = null,
        @SerializedName("method_title")
        val methodTitle: String? = null,
        @SerializedName("meta_data")
        val metaData: List<WCMetaData>? = null
    )

    data class WCRefundFeeLine(
        @SerializedName("id")
        val id: Long,
        @SerializedName("name")
        val name: String,
        @SerializedName("total")
        val total: BigDecimal,
        @SerializedName("total_tax")
        val totalTax: BigDecimal,
        @SerializedName("meta_data")
        val metaData: List<WCMetaData>? = null
    )
}
