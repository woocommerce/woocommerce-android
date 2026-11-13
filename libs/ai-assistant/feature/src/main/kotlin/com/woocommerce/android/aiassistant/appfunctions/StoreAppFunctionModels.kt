package com.woocommerce.android.aiassistant.appfunctions

import androidx.appfunctions.AppFunctionSerializable

/** Sales totals for one period in the store that is open in the WooCommerce app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class SalesSummary(
    /** Name of the store these totals come from. */
    val storeName: String,
    /** The period that was totaled: today, yesterday, last_7_days or this_month. */
    val period: String,
    /** First day of the period, as YYYY-MM-DD in the store's time zone. */
    val startDate: String,
    /** Last day of the period, as YYYY-MM-DD in the store's time zone. */
    val endDate: String,
    /** Currency code of the amounts, such as USD. Null when the store doesn't report one. */
    val currency: String?,
    /** Net sales for the period, as shown in WooCommerce Analytics, as a decimal number. Null when unavailable. */
    val netSales: String?,
    /** Number of orders placed in the period. Null when unavailable. */
    val orderCount: Int?,
    /** Average order value for the period, as a decimal number. Null when unavailable. */
    val averageOrderValue: String?,
    /** Number of items sold in the period. Null when unavailable. */
    val itemsSold: Int?,
)

/** Orders found in the store that is open in the WooCommerce app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OrderSearchResult(
    /** Name of the store that was searched. */
    val storeName: String,
    /** The matching orders, newest first. Empty when nothing matches. */
    val orders: List<OrderSummary>,
    /** True when the list reached the limit, so more orders may match. */
    val mayHaveMore: Boolean,
)

/** One order in a list of orders. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OrderSummary(
    /** The order ID. Pass it to getOrderDetails or openOrderInApp. */
    val orderId: Long,
    /** The order number the merchant and the customer see. It can differ from the order ID. */
    val orderNumber: String,
    /** The order status, such as processing or completed. */
    val status: String,
    /** The order total as a decimal number, such as 42.50. */
    val total: String,
    /** Currency code of the total, such as USD. */
    val currency: String,
    /** When the order was created, in UTC, as ISO 8601. */
    val dateCreated: String,
    /** The customer's billing name. Empty when the order has none. */
    val customerName: String,
)

/** One order with its items, from the store that is open in the WooCommerce app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OrderDetails(
    /** Name of the store the order belongs to. */
    val storeName: String,
    /** The order ID. */
    val orderId: Long,
    /** The order number the merchant and the customer see. */
    val orderNumber: String,
    /** The order status, such as processing or completed. */
    val status: String,
    /** When the order was created, in UTC, as ISO 8601. */
    val dateCreated: String,
    /** When the order was paid, in UTC, as ISO 8601. Null when it isn't paid. */
    val datePaid: String?,
    /** The order total, including tax and shipping, as a decimal number. */
    val total: String,
    /** Currency code of the amounts, such as USD. */
    val currency: String,
    /** Shipping cost as a decimal number. */
    val shippingTotal: String,
    /** Discount amount as a decimal number. */
    val discountTotal: String,
    /** Tax amount as a decimal number. */
    val totalTax: String,
    /** The customer's billing name. Empty when the order has none. */
    val customerName: String,
    /** How many items the order has in total. */
    val itemCount: Int,
    /** The first 10 items of the order. */
    val items: List<OrderItem>,
)

/** One item of an order. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class OrderItem(
    /** Product name as shown on the order. */
    val name: String,
    /** Quantity ordered. */
    val quantity: Float,
    /** Total for this item as a decimal number. */
    val total: String,
    /** ID of the product, for findProducts or requestStockChange. Null when the product no longer exists. */
    val productId: Long?,
    /** ID of the variation when the item is a variation of a variable product, otherwise null. */
    val variationId: Long?,
)

/** Products found in the store that is open in the WooCommerce app. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ProductSearchResult(
    /** Name of the store that was searched. */
    val storeName: String,
    /** Currency code of the prices, such as USD. Null when the store doesn't report one. */
    val currency: String?,
    /** The matching products. Empty when nothing matches. */
    val products: List<ProductSummary>,
    /** True when more products may match than were returned. */
    val mayHaveMore: Boolean,
)

/** One product in a list. */
@AppFunctionSerializable(isDescribedByKDoc = true)
data class ProductSummary(
    /** The product ID. Pass it to requestStockChange. */
    val productId: Long,
    /** Product name. */
    val name: String,
    /** The product SKU. Null when it has none. */
    val sku: String?,
    /**
     * Product type, such as simple, variable, grouped or external. Only simple products and variations of variable
     * products can have their stock changed.
     */
    val type: String,
    /** Publication status, such as publish or draft. */
    val status: String,
    /** Current price as a decimal number. Empty when the product has no price. */
    val price: String,
    /** Stock status: instock, outofstock or onbackorder. */
    val stockStatus: String,
    /**
     * Units in stock when the store tracks stock for this product, otherwise null. Variable products usually track
     * stock per variation.
     */
    val stockQuantity: Int?,
)
