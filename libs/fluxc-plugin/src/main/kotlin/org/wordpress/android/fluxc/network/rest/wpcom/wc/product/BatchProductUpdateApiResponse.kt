package org.wordpress.android.fluxc.network.rest.wpcom.wc.product

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.annotations.JsonAdapter
import org.wordpress.android.fluxc.model.ProductWithMetaData
import org.wordpress.android.fluxc.network.Response
import java.lang.reflect.Type

data class BatchProductUpdateApiResponse(
    val update: List<ProductResponse>
) : Response {
    @JsonAdapter(ProductResponseDeserializer::class)
    sealed class ProductResponse {
        data class Success(
            val product: ProductDto
        ) : ProductResponse()

        data class Error(
            val id: Long,
            val error: ErrorResponse
        ) : ProductResponse()
    }

    data class ErrorResponse(
        val code: String,
        val message: String,
        val data: ErrorData
    )

    data class ErrorData(
        val status: Int
    )

    private class ProductResponseDeserializer : JsonDeserializer<ProductResponse> {
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): ProductResponse {
            val jsonObject = json.asJsonObject

            return if (jsonObject.has("error")) {
                ProductResponse.Error(
                    id = jsonObject.get("id").asLong,
                    error = context.deserialize(jsonObject.get("error"), ErrorResponse::class.java)
                )
            } else {
                ProductResponse.Success(
                    context.deserialize(jsonObject, ProductDto::class.java)
                )
            }
        }
    }
}

data class BatchProductUpdateResult(
    val update: List<ProductResponse>
) {
    sealed class ProductResponse {
        data class Success(
            val product: ProductWithMetaData
        ) : ProductResponse()

        data class Error(
            val id: Long,
            val error: BatchProductUpdateApiResponse.ErrorResponse
        ) : ProductResponse()
    }
}
