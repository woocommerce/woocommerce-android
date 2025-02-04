package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonElement
import org.wordpress.android.fluxc.model.order.CouponLine
import org.wordpress.android.fluxc.network.Response

@Suppress("PropertyName", "VariableNaming")
class OrderDto : Response {
    @Suppress("ConstructorParameterNaming")
    data class Billing(
        val first_name: String? = null,
        val last_name: String? = null,
        val company: String? = null,
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postcode: String? = null,
        val country: String? = null,
        val email: String? = null,
        val phone: String? = null
    )

    @Suppress("ConstructorParameterNaming")
    data class Shipping(
        val first_name: String? = null,
        val last_name: String? = null,
        val company: String? = null,
        val address_1: String? = null,
        val address_2: String? = null,
        val city: String? = null,
        val state: String? = null,
        val postcode: String? = null,
        val country: String? = null,
        val phone: String? = null
    )

    class Refund {
        val total: String? = null
    }

    val customer_id: Long? = null
    val billing: Billing? = null
    val coupon_lines: List<CouponLine>? = null
    val currency: String? = null
    val order_key: String? = null
    val customer_note: String? = null
    val date_created_gmt: String? = null
    val date_modified_gmt: String? = null
    val date_paid_gmt: String? = null
    val discount_total: String? = null
    // Same as shipping_lines, it's a list of objects
    val fee_lines: JsonElement? = null
    val id: Long? = null
    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See OrderEntity.LineItem
    val line_items: JsonElement? = null
    val number: String? = null
    val payment_method: String? = null
    val payment_method_title: String? = null
    val prices_include_tax: Boolean = false
    val refunds: List<Refund>? = null
    val shipping: Shipping? = null
    // This is actually a list of objects. We're storing this as JSON initially, and it will be deserialized on demand.
    // See OrderEntity.ShippingLines
    val shipping_lines: JsonElement? = null
    val shipping_total: String? = null
    val status: String? = null
    val total: String? = null
    val total_tax: String? = null
    val meta_data: JsonElement? = null
    val tax_lines: JsonElement? = null
    val payment_url: String? = null
    val is_editable: Boolean? = null
    val needs_payment: Boolean? = null
    val needs_processing: Boolean? = null
    val shipping_tax: String? = null
}
