package com.woocommerce.commons.wear

enum class MessagePath(val value: String) {
    REQUEST_TOKEN("/request-token"),
    REQUEST_SITE("/request-site")
}

enum class DataPath(val value: String) {
    TOKEN_DATA("/token-data"),
    SITE_DATA("/site-data")
}

enum class DataParameters(val value: String) {
    TOKEN("token"),
    SITE_ID("site-id"),
    SITE_DATA("site-data"),
    TIMESTAMP("timestamp")
}
