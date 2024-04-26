package com.woocommerce.commons.wear

enum class MessagePath(val value: String) {
    REQUEST_SITE("/request-site")
}

enum class DataPath(val value: String) {
    SITE_DATA("/site-data")
}

enum class DataParameters(val value: String) {
    TOKEN("token"),
    SITE_JSON("site-json"),
    TIMESTAMP("timestamp")
}
