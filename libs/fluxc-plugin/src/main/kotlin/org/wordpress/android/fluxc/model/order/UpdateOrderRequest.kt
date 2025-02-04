package org.wordpress.android.fluxc.model.order

import org.wordpress.android.fluxc.model.WCOrderStatusModel

data class UpdateOrderRequest(
    val customerId: Long? = null,
    val status: WCOrderStatusModel? = null,
    val lineItems: List<Map<String, Any>>? = null,
    val shippingAddress: OrderAddress.Shipping? = null,
    val billingAddress: OrderAddress.Billing? = null,
    val feeLines: List<FeeLine>? = null,
    val couponLines: List<CouponLine>? = null,
    val shippingLines: List<ShippingLine>? = null,
    val customerNote: String? = null,
    val giftCard: String? = null
)
