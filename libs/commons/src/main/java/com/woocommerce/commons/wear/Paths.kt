package com.woocommerce.commons.wear

enum class MessagePath(val value: String) {
    REQUEST_SITE("/request-site"),
    REQUEST_STATS("/request-stats")
}

enum class DataPath(val value: String) {
    SITE_DATA("/site-data"),
    STATS_DATA("/stats-data")
}

enum class DataParameters(val value: String) {
    // Store credentials data
    TOKEN("token"),
    SITE_JSON("site-json"),
    TIMESTAMP("timestamp"),

    // Stats data
    TOTAL_REVENUE("total-revenue"),
    ORDERS_COUNT("orders-count"),
    VISITORS_TOTAL("visitors-total"),
    CONVERSION_RATE("conversion-rate")
}
