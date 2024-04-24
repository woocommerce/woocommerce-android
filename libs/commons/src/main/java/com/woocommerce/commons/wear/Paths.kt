package com.woocommerce.commons.wear

enum class MessagePath(val value: String) {
    START_AUTH("/start-auth")
}

enum class DataPath(val value: String) {
    AUTH_DATA("/auth-data")
}

enum class DataParameters(val value: String) {
    TOKEN("token"),
    TIMESTAMP("timestamp")
}
