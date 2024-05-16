package com.woocommerce.commons.wear

enum class MessagePath(val value: String) {
    REQUEST_SITE("/request-site"),
    REQUEST_STATS("/request-stats"),
    REQUEST_ORDERS("/request-orders"),
    REQUEST_ORDER_PRODUCTS("/request-order-products")
}

enum class DataPath(val value: String) {
    SITE_DATA("/site-data"),
    STATS_DATA("/stats-data"),
    ORDERS_DATA("/orders-data"),
    ORDER_PRODUCTS_DATA("/order-products-data")
}

enum class DataParameters(val value: String) {
    TIMESTAMP("timestamp"),

    // Store credentials data
    TOKEN("token"),
    SITE_JSON("site-json"),

    // Stats data
    TOTAL_REVENUE("total-revenue"),
    ORDERS_COUNT("orders-count"),
    VISITORS_TOTAL("visitors-total"),
    CONVERSION_RATE("conversion-rate"),

    // Orders data
    ORDERS_JSON("orders-json"),

    // Order products data
    ORDER_ID("order-id"),
    ORDER_PRODUCTS_JSON("order-products-json")
}