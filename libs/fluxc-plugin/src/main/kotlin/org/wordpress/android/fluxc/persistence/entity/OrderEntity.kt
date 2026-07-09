package org.wordpress.android.fluxc.persistence.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.order.CouponLine
import org.wordpress.android.fluxc.model.order.FeeLine
import org.wordpress.android.fluxc.model.order.GiftCardLine
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
    @ColumnInfo(name = "giftCards", defaultValue = "")
    val giftCards: String = "",
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
    @ColumnInfo(name = "createdVia", defaultValue = "")
    val createdVia: String = "",
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
    suspend fun getLineItemList(): List<LineItem> {
        return getCachedOrParse(lineItems)
    }

    /**
     * Returns the order subtotal (the sum of the subtotals of each line item in the order).
     */
    suspend fun getOrderSubtotal(): Double {
        return getLineItemList().sumOf { it.subtotal?.toDoubleOrNull() ?: 0.0 }
    }

    /**
     * Deserializes the JSON contained in [shippingLines] into a list of [ShippingLine] objects.
     */
    suspend fun getShippingLineList(): List<ShippingLine> {
        return getCachedOrParse(shippingLines)
    }

    /**
     * Deserializes the JSON contained in [feeLines] into a list of [FeeLine] objects.
     */
    suspend fun getFeeLineList(): List<FeeLine> {
        return getCachedOrParse(feeLines)
    }

    /**
     * Deserializes the JSON contained in [couponLines] into a list of [CouponLine] objects.
     */
    suspend fun getCouponLineList(): List<CouponLine> {
        return getCachedOrParse(couponLines)
    }

    /**
     * Deserializes the JSON contained in [giftCards] into a list of [GiftCardLine] objects.
     */
    suspend fun getGiftCardList(): List<GiftCardLine> {
        return getCachedOrParse(giftCards)
    }

    /**
     * Deserializes the JSON contained in [taxLines] into a list of [TaxLine] objects.
     * Returns an empty list if deserialization fails.
     */
    suspend fun getTaxLineList(): List<TaxLine> {
        return getCachedOrParse(taxLines)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend inline fun <reified T> getCachedOrParse(json: String): List<T> {
        return parseJsonListSafely<T>(json)
    }

    private suspend inline fun <reified T> parseJsonListSafely(json: String): List<T> = withContext(Dispatchers.IO) {
        try {
            val responseType = object : TypeToken<List<T>>() {}.type
            gson.fromJson(json, responseType) as? List<T> ?: emptyList()
        } catch (e: JsonSyntaxException) {
            @Suppress("PrintStackTrace")
            e.printStackTrace()
            emptyList()
        } catch (e: NumberFormatException) {
            @Suppress("PrintStackTrace")
            e.printStackTrace()
            emptyList()
        }
    }
}
