package org.wordpress.android.fluxc.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.order.CouponLine
import org.wordpress.android.fluxc.model.order.FeeLine
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.OrderAddress
import org.wordpress.android.fluxc.model.order.ShippingLine
import org.wordpress.android.fluxc.model.order.TaxLine
import org.wordpress.android.fluxc.persistence.converters.WCMetaDataConverter
import java.math.BigDecimal

@Entity(
    tableName = "OrderEntity",
    indices = [Index(
        value = ["localSiteId", "orderId"]
    )],
    primaryKeys = ["localSiteId", "orderId"]
)
data class OrderEntity(
    @ColumnInfo(name = "localSiteId")
    val localSiteId: LocalId,
    val orderId: Long,
    val number: String = "", // The, order number to display to the user
    val status: String = "",
    val currency: String = "",
    val orderKey: String = "",
    val dateCreated: String = "", // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    val dateModified: String = "", // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z
    val total: String = "", // Complete total, including taxes
    val totalTax: String = "", // The total amount of tax (from products, shipping, discounts, etc.)
    val shippingTotal: String = "", // The total shipping cost (excluding tax)
    val paymentMethod: String = "", // Payment method code e.g. 'cod' 'stripe'
    val paymentMethodTitle: String = "", // Displayable payment method e.g. 'Cash on delivery' 'Credit Card (Stripe)'
    val datePaid: String = "",
    val pricesIncludeTax: Boolean = false,
    val customerNote: String = "", // Note left by the customer during order submission
    val discountTotal: String = "",
    val discountCodes: String = "",
    val refundTotal: BigDecimal = BigDecimal.ZERO, // The total refund value for this order (usually a negative number)
    @ColumnInfo(name = "customerId", defaultValue = "0")
    val customerId: Long = 0,
    val billingFirstName: String = "",
    val billingLastName: String = "",
    val billingCompany: String = "",
    val billingAddress1: String = "",
    val billingAddress2: String = "",
    val billingCity: String = "",
    val billingState: String = "",
    val billingPostcode: String = "",
    val billingCountry: String = "",
    val billingEmail: String = "",
    val billingPhone: String = "",
    val shippingFirstName: String = "",
    val shippingLastName: String = "",
    val shippingCompany: String = "",
    val shippingAddress1: String = "",
    val shippingAddress2: String = "",
    val shippingCity: String = "",
    val shippingState: String = "",
    val shippingPostcode: String = "",
    val shippingCountry: String = "",
    val shippingPhone: String = "",
    val lineItems: String = "",
    val shippingLines: String = "",
    val feeLines: String = "",
    val taxLines: String = "",
    @ColumnInfo(name = "couponLines", defaultValue = "")
    val couponLines: String = "",
    // this is a small subset of the metadata, see OrderMetaDataEntity for full metadata
    @field:TypeConverters(WCMetaDataConverter::class)
    val metaData: List<WCMetaData> = emptyList(),
    @ColumnInfo(name = "paymentUrl", defaultValue = "")
    val paymentUrl: String = "",
    @ColumnInfo(name = "isEditable", defaultValue = "1")
    val isEditable: Boolean = true,
    @ColumnInfo(name = "needsPayment")
    val needsPayment: Boolean? = null,
    @ColumnInfo(name = "needsProcessing")
    val needsProcessing: Boolean? = null,
    @ColumnInfo(name = "giftCardCode", defaultValue = "")
    val giftCardCode: String = "",
    @ColumnInfo(name = "giftCardAmount", defaultValue = "")
    val giftCardAmount: String = "",
    @ColumnInfo(name = "shippingTax", defaultValue = "")
    val shippingTax: String = "",
) {
    companion object {
        private val gson by lazy { Gson() }
    }

    /**
     * Returns true if there are shipping details defined for this order,
     * which are different from the billing details.
     *
     * If no separate shipping details are defined, the billing details should be used instead,
     * as the shippingX properties will be empty.
     */
    fun hasSeparateShippingDetails() = shippingCountry.isNotEmpty()

    /**
     * Returns the billing details wrapped in a [OrderAddress].
     */
    fun getBillingAddress() = OrderAddress.Billing(this)

    /**
     * Returns the shipping details wrapped in a [OrderAddress].
     */
    fun getShippingAddress() = OrderAddress.Shipping(this)

    /**
     * Deserializes the JSON contained in [lineItems] into a list of [LineItem] objects.
     */
    fun getLineItemList(): List<LineItem> {
        val responseType = object : TypeToken<List<LineItem>>() {}.type
        return gson.fromJson(lineItems, responseType) as? List<LineItem> ?: emptyList()
    }

    /**
     * Returns the order subtotal (the sum of the subtotals of each line item in the order).
     */
    fun getOrderSubtotal(): Double {
        return getLineItemList().sumByDouble { it.subtotal?.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Deserializes the JSON contained in [shippingLines] into a list of [ShippingLine] objects.
     */
    fun getShippingLineList(): List<ShippingLine> {
        val responseType = object : TypeToken<List<ShippingLine>>() {}.type
        return gson.fromJson(shippingLines, responseType) as? List<ShippingLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [feeLines] into a list of [FeeLine] objects.
     */
    fun getFeeLineList(): List<FeeLine> {
        val responseType = object : TypeToken<List<FeeLine>>() {}.type
        return gson.fromJson(feeLines, responseType) as? List<FeeLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [couponLines] into a list of [CouponLine] objects.
     */
    fun getCouponLineList(): List<CouponLine> {
        val responseType = object : TypeToken<List<CouponLine>>() {}.type
        return gson.fromJson(couponLines, responseType) as? List<CouponLine> ?: emptyList()
    }

    /**
     * Deserializes the JSON contained in [taxLines] into a list of [TaxLine] objects.
     */
    fun getTaxLineList(): List<TaxLine> {
        val responseType = object : TypeToken<List<TaxLine>>() {}.type
        return gson.fromJson(taxLines, responseType) as? List<TaxLine> ?: emptyList()
    }

    fun isMultiShippingLinesAvailable() = getShippingLineList().size > 1
}
