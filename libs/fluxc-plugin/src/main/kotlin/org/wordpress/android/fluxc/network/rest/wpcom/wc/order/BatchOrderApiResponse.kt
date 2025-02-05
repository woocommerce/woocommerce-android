package org.wordpress.android.fluxc.network.rest.wpcom.wc.order

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import java.lang.reflect.Type
import org.wordpress.android.fluxc.network.Response

/**
 * Represents the response from WooCommerce's Batch Order Update API endpoint.
 * https://woocommerce.github.io/woocommerce-rest-api-docs/?shell#batch-update-orders
 *
 * While the WooCommerce REST API orders batch endpoint supports three operations at once
 * (create, update, delete), this class specifically handles only the "update" operation
 * responses, because we don't yet support the other operations.
 *
 * The response contains a list of order updates, where each update can be
 * either successful or failed.
 * 1. Success: Contains the complete updated order data (OrderDto)
 * 2. Error: Contains the failed order ID and error details
 *
 *  Also refer to the orders-batch.json file in test resources.
 *
 * Example successful response:
 * {
 *   "update": [{
 *     "id": 1032,
 *     "status": "completed",
 *     // ... other order fields
 *   }]
 * }
 *
 * Example error response:
 * {
 *   "update": [{
 *     "id": "525",
 *     "error": {
 *       "code": "woocommerce_rest_shop_order_invalid_id",
 *       "message": "Invalid ID.",
 *       "data": { "status": 400 }
 *     }
 *   }]
 * }
 *
 */
data class BatchOrderApiResponse(
    val update: List<OrderResponse>
) : Response {
    @JsonAdapter(OrderResponseDeserializer::class)
    sealed class OrderResponse {
        data class Success(
            val order: OrderDto
        ) : OrderResponse()

        data class Error(
            val id: Long,
            val error: ErrorResponse
        ) : OrderResponse()
    }

    data class ErrorResponse(
        val code: String,
        val message: String,
        val data: ErrorData
    )

    data class ErrorData(
        val status: Int
    )

    private class OrderResponseDeserializer : JsonDeserializer<OrderResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): OrderResponse {
            val jsonObject = json.asJsonObject

            return if (jsonObject.has("error")) {
                OrderResponse.Error(
                    id = jsonObject.get("id").asLong,
                    error = context.deserialize(jsonObject.get("error"), ErrorResponse::class.java)
                )
            } else {
                OrderResponse.Success(
                    context.deserialize(jsonObject, OrderDto::class.java)
                )
            }
        }
    }
}
