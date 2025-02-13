package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

/**
 * Standard Core WooCommerce product tax statuses
 */
enum class CoreProductTaxStatus(val value: String) {
    TAXABLE("taxable"),
    SHIPPING("shipping"),
    NONE("none");

    companion object {
        private val valueMap = values().associateBy(CoreProductTaxStatus::value)

        /**
         * Convert the base value into the associated CoreProductTaxStatus object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
