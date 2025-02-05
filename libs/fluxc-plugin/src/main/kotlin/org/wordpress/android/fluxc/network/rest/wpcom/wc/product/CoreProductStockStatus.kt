package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

/**
 * Standard Core WooCommerce product stock statuses
 */
enum class CoreProductStockStatus(val value: String) {
    IN_STOCK("instock"),
    LOW_STOCK("lowstock"),
    OUT_OF_STOCK("outofstock"),
    ON_BACK_ORDER("onbackorder");

    companion object {
        private val valueMap = entries.associateBy(CoreProductStockStatus::value)
        val ALL_VALUES = valueMap.keys

        val FILTERABLE_VALUES = setOf(IN_STOCK, OUT_OF_STOCK, ON_BACK_ORDER)
        /**
         * Convert the base value into the associated CoreProductStockStatus object
         */
        fun fromValue(value: String) = valueMap[value]
    }
}
