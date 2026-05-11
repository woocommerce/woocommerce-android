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
        COUPON("coupon"),
        CUSTOM_AMOUNT("custom_amount"),
        LOADING("loading"),
        ERROR("error");

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
        POPULAR_PRODUCTS("pre_search"),
        BARCODE_SCANNER("scanner");

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

    enum class SyncType(val value: String) {
        FULL("full"),
        INCREMENTAL("incremental");

        override fun toString(): String {
            return value
        }

        companion object {
            const val SYNC_TYPE = "sync_type"
        }
    }

    enum class SyncErrorType(val value: String) {
        NETWORK_ERROR("network_error"),
        DATABASE_ERROR("database_error"),
        INVALID_RESPONSE("invalid_response"),
        UNEXPECTED_ERROR("unexpected_error"),
        CATALOG_GENERATION_TIMEOUT("catalog_generation_timeout");

        override fun toString(): String {
            return value
        }

        companion object {
            const val ERROR_TYPE = "error_type"
        }
    }

    enum class SyncSkipReason(val value: String) {
        POS_NOT_OPENED_30_DAYS("pos_not_opened_30_days"),
        SYNC_NOT_REQUIRED("sync_not_required"),
        LOCAL_CATALOG_DISABLED("local_catalog_disabled"),
        CHECKING_SYNC_REQUIREMENT_FAILED("checking_sync_requirement_failed"),
        SITE_NOT_SELECTED("site_not_selected");

        override fun toString(): String {
            return value
        }

        companion object {
            const val SKIP_REASON = "reason"
        }
    }
}
