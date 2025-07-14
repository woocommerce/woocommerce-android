package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

/**
 * Standard Core WooCommerce product back order options
 */
enum class CoreProductBackOrders(val value: String) {
    NO("no"),
    NOTIFY("notify"),
    YES("yes");

    companion object {
        private val valueMap = values().associateBy(CoreProductBackOrders::value)

        /**
         * Convert the base value into the associated CoreProductBackOrders object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
