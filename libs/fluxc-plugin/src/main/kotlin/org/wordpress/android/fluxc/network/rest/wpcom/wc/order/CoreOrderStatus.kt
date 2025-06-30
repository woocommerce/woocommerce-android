package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

/**
 * Standard Core WooCommerce order statuses
 */
enum class CoreOrderStatus(val value: String) {
    PENDING("pending"),
    PROCESSING("processing"),
    ON_HOLD("on-hold"),
    COMPLETED("completed"),
    CANCELLED("cancelled"),
    REFUNDED("refunded"),
    FAILED("failed");

    companion object {
        private val valueMap = CoreOrderStatus.values().associateBy(CoreOrderStatus::value)

        /**
         * Convert the base value into the associated CoreOrderStatus object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
