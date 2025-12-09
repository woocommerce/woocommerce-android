//package com.woocommerce.android.ui.orders.creation
//
//import org.assertj.core.api.Assertions.assertThat
//import org.junit.Test
//import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
//import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
//import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
//
//class OrderCreationErrorTest {
//
//    @Test
//    fun `given variation error with variation_id, when fromWooError, then extracts variation_id`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.API_ERROR,
//            original = GenericErrorType.UNKNOWN,
//            message = "Invalid variation ID",
//            apiErrorCode = "order_item_product_invalid_variation_id",
//            errorData = mapOf("status" to 400, "variation_id" to 5360)
//        )
//
//        // When
//        val result = OrderCreationError.fromWooError(wooError)
//
//        // Then
//        assertThat(result.deletedVariationId).isEqualTo(5360L)
//        assertThat(result.deletedProductId).isNull()
//        assertThat(result.isVariationDeleted).isTrue()
//        assertThat(result.hasDeletedItems).isTrue()
//    }
//
//    @Test
//    fun `given variation error without variation_id, when fromWooError, then no variation_id extracted`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.API_ERROR,
//            original = GenericErrorType.UNKNOWN,
//            message = "Invalid variation ID",
//            apiErrorCode = "order_item_product_invalid_variation_id",
//            errorData = mapOf("status" to 400) // No variation_id
//        )
//
//        // When
//        val result = OrderCreationError.fromWooError(wooError)
//
//        // Then
//        assertThat(result.deletedVariationId).isNull()
//        assertThat(result.isVariationDeleted).isFalse()
//        assertThat(result.hasDeletedItems).isFalse()
//    }
//
//    @Test
//    fun `given product error with product_id, when fromWooError, then extracts product_id`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.API_ERROR,
//            original = GenericErrorType.UNKNOWN,
//            message = "Invalid product ID",
//            apiErrorCode = "order_item_product_invalid_product_id",
//            errorData = mapOf("status" to 400, "product_id" to 1234)
//        )
//
//        // When
//        val result = OrderCreationError.fromWooError(wooError)
//
//        // Then
//        assertThat(result.deletedProductId).isEqualTo(1234L)
//        assertThat(result.deletedVariationId).isNull()
//        assertThat(result.isProductDeleted).isTrue()
//        assertThat(result.hasDeletedItems).isTrue()
//    }
//
//    @Test
//    fun `given variation_id as different number types, when fromWooError, then converts to Long`() {
//        val testCases = listOf(
//            5360L to 5360L,           // Long
//            5360 to 5360L,            // Int
//            5360.0 to 5360L,          // Double
//            5360.0f to 5360L,         // Float
//            "5360" to null            // String - should be null
//        )
//
//        testCases.forEach { (input, expected) ->
//            // Given
//            val wooError = WooError(
//                type = WooErrorType.API_ERROR,
//                original = GenericErrorType.UNKNOWN,
//                apiErrorCode = "order_item_product_invalid_variation_id",
//                errorData = mapOf("variation_id" to input)
//            )
//
//            // When
//            val result = OrderCreationError.fromWooError(wooError)
//
//            // Then
//            assertThat(result.deletedVariationId).isEqualTo(expected)
//        }
//    }
//
//    @Test
//    fun `given non-order error, when fromWooError, then returns generic error`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.TIMEOUT,
//            original = GenericErrorType.TIMEOUT,
//            message = "Request timeout"
//        )
//
//        // When
//        val result = OrderCreationError.fromWooError(wooError)
//
//        // Then
//        assertThat(result.deletedVariationId).isNull()
//        assertThat(result.deletedProductId).isNull()
//        assertThat(result.hasDeletedItems).isFalse()
//        assertThat(result.baseError).isEqualTo(wooError)
//    }
//
//    @Test
//    fun `given variation error, when isDeletedVariationError, then returns true`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.API_ERROR,
//            original = GenericErrorType.UNKNOWN,
//            apiErrorCode = "order_item_product_invalid_variation_id",
//            errorData = mapOf("variation_id" to 5360)
//        )
//
//        // When & Then
//        assertThat(OrderCreationError.isDeletedVariationError(wooError)).isTrue()
//    }
//
//    @Test
//    fun `given product error, when isDeletedProductError, then returns true`() {
//        // Given
//        val wooError = WooError(
//            type = WooErrorType.API_ERROR,
//            original = GenericErrorType.UNKNOWN,
//            apiErrorCode = "order_item_product_invalid_product_id",
//            errorData = mapOf("product_id" to 1234)
//        )
//
//        // When & Then
//        assertThat(OrderCreationError.isDeletedProductError(wooError)).isTrue()
//    }
//}
