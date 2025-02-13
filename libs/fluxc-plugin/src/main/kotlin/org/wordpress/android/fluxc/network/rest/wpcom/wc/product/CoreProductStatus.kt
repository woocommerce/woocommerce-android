package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

enum class CoreProductStatus(val value: String) {
    DRAFT("draft"),
    PENDING("pending"),
    PRIVATE("private"),
    PUBLISH("publish"),
    TRASH("trash");

    companion object {
        private val valueMap = values().associateBy(CoreProductStatus::value)

        /**
         * Convert the base value into the associated CoreProductStatus object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
