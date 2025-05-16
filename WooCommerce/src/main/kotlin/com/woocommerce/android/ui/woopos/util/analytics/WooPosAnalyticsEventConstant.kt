package com.woocommerce.android.ui.woopos.util.analytics

object WooPosAnalyticsEventConstant {
    enum class ItemsListProductType(val value: String) {
        SIMPLE("simple"),
        VARIATION("variation");

        override fun toString(): String {
            return value
        }

        companion object {
            const val PRODUCT_TYPE = "product_type"
        }
    }

    enum class ItemsListItemType(val value: String) {
        PRODUCT("product"),
        COUPON("coupon");

        override fun toString(): String {
            return value
        }
        companion object {
            const val ITEM_TYPE = "item_type"
        }
    }

    enum class ItemsListSource(val value: String) {
        PRODUCT("product"),
        VARIATION("variation"),
        COUPON("coupon");

        override fun toString(): String {
            return value
        }

        companion object {
            const val SOURCE = "source"
        }
    }

    enum class ItemsListSourceType(val value: String) {
        LIST("list"),
        SEARCH_RESULT("search"),
        SEARCH_RESULT_LOCAL("search_result_local"),
        POPULAR_PRODUCTS("pre_search");

        override fun toString(): String {
            return value
        }

        companion object {
            const val SOURCE_TYPE = "source_type"
        }
    }

    enum class ItemsHeaderType(val value: String) {
        PRODUCT("product"),
        COUPON("coupon");

        override fun toString(): String {
            return value
        }

        companion object {
            const val HEADER_TYPE = "type"
        }
    }

    enum class CartSource(val value: String) {
        CART("cart"),
        ERROR("error");

        override fun toString(): String {
            return value
        }

        companion object {
            const val CART_SOURCE = "source"
        }
    }
}
