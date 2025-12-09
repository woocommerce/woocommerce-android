//package com.woocommerce.android.ui.orders.creation
//
//import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
//
///**
// * Domain-specific error handler for order creation.
// * Extracts and interprets order-specific error information from generic WooError.
// */
//data class OrderCreationError(
//    val baseError: WooError,
//    val deletedVariationId: Long? = null,
//    val deletedProductId: Long? = null
//) {
//    companion object {
//        private const val ERROR_CODE_INVALID_VARIATION = "order_item_product_invalid_variation_id"
//        private const val ERROR_CODE_INVALID_PRODUCT = "order_item_product_invalid_product_id"
//        private const val KEY_VARIATION_ID = "variation_id"
//        private const val KEY_PRODUCT_ID = "product_id"
//
//        /**
//         * Creates an OrderCreationError from a WooError, extracting order-specific
//         * error information like deleted variation IDs.
//         */
//        fun fromWooError(error: WooError): OrderCreationError {
//            val variationId = when (error.apiErrorCode) {
//                ERROR_CODE_INVALID_VARIATION -> {
//                    // Extract variation_id from errorData map
//                    // The errorData contains: { "status": 400, "variation_id": 5360 }
//                    error.errorData?.get(KEY_VARIATION_ID)?.let { value ->
//                        when (value) {
//                            is Long -> value
//                            is Int -> value.toLong()
//                            is Double -> value.toLong()
//                            is Float -> value.toLong()
//                            else -> null
//                        }
//                    }
//                }
//                else -> null
//            }
//
//            val productId = when (error.apiErrorCode) {
//                ERROR_CODE_INVALID_PRODUCT -> {
//                    // In case the API also returns deleted product IDs in the future
//                    error.errorData?.get(KEY_PRODUCT_ID)?.let { value ->
//                        when (value) {
//                            is Long -> value
//                            is Int -> value.toLong()
//                            is Double -> value.toLong()
//                            is Float -> value.toLong()
//                            else -> null
//                        }
//                    }
//                }
//                else -> null
//            }
//
//            return OrderCreationError(
//                baseError = error,
//                deletedVariationId = variationId,
//                deletedProductId = productId
//            )
//        }
//
//        /**
//         * Helper function to check if this is a deleted variation error
//         */
//        fun isDeletedVariationError(error: WooError): Boolean {
//            return error.apiErrorCode == ERROR_CODE_INVALID_VARIATION &&
//                error.errorData?.containsKey(KEY_VARIATION_ID) == true
//        }
//
//        /**
//         * Helper function to check if this is a deleted product error
//         */
//        fun isDeletedProductError(error: WooError): Boolean {
//            return error.apiErrorCode == ERROR_CODE_INVALID_PRODUCT &&
//                error.errorData?.containsKey(KEY_PRODUCT_ID) == true
//        }
//    }
//
//    /**
//     * Returns true if this error is due to a deleted variation
//     */
//    val isVariationDeleted: Boolean
//        get() = deletedVariationId != null
//
//    /**
//     * Returns true if this error is due to a deleted product
//     */
//    val isProductDeleted: Boolean
//        get() = deletedProductId != null
//
//    /**
//     * Returns true if this error indicates any deleted item (product or variation)
//     * that should be handled the same way as existing deleted product errors
//     */
//    val hasDeletedItems: Boolean
//        get() = isVariationDeleted || isProductDeleted
//}
