package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

enum class CoreProductVisibility(val value: String) {
    VISIBLE("visible"),
    CATALOG("catalog"),
    SEARCH("search"),
    HIDDEN("hidden");

    companion object {
        private val valueMap = values().associateBy(CoreProductVisibility::value)

        /**
         * Convert the base value into the associated CoreProductVisibility object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
