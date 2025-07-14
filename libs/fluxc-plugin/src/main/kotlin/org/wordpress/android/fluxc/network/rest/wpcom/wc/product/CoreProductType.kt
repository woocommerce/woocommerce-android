package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

enum class CoreProductType(val value: String) {
    SIMPLE("simple"),
    GROUPED("grouped"),
    EXTERNAL("external"),
    VARIABLE("variable"),
    BUNDLE("bundle");

    companion object {
        private val valueMap = values().associateBy(CoreProductType::value)

        /**
         * Convert the base value into the associated CoreProductType object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
