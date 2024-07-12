package com.woocommerce.android.ui.products.ai

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import org.json.JSONArray
import org.json.JSONObject
import java.math.BigDecimal

data class AIProductModel(
    val names: List<String>,
    val descriptions: List<String>,
    val shortDescriptions: List<String>,
    val isVirtual: Boolean,
    val price: BigDecimal,
    val shipping: Shipping,
    val categories: List<ProductCategory>? = null,
    val tags: List<ProductTag>? = null
) {
    @Suppress("unused")
    fun toProduct(
        variant: Int
    ): Product = ProductHelper.getDefaultNewProduct(SIMPLE, isVirtual).copy(
        name = names[variant],
        description = descriptions[variant],
        shortDescription = shortDescriptions[variant],
        regularPrice = price,
        categories = categories.orEmpty(),
        tags = tags.orEmpty(),
        weight = shipping.weight,
        height = shipping.height,
        length = shipping.length,
        width = shipping.width,
        status = DRAFT
    )

    data class Shipping(
        val weight: Float,
        val height: Float,
        val length: Float,
        val width: Float
    )

    companion object {
        fun fromJson(
            json: String,
            existingCategories: List<ProductCategory>,
            existingTags: List<ProductTag>
        ): AIProductModel {
            val jsonModel = JSONObject(json)
            return AIProductModel(
                names = jsonModel.getJSONArray("names").toList(),
                descriptions = jsonModel.getJSONArray("descriptions").toList(),
                shortDescriptions = jsonModel.getJSONArray("short_descriptions").toList(),
                isVirtual = jsonModel.getBoolean("virtual"),
                price = BigDecimal(jsonModel.getString("price")),
                shipping = jsonModel.getJSONObject("shipping").run {
                    Shipping(
                        weight = getDouble("weight").toFloat(),
                        height = getDouble("height").toFloat(),
                        length = getDouble("length").toFloat(),
                        width = getDouble("width").toFloat()
                    )
                },
                categories = jsonModel.getJSONArray("categories").toList().map { name ->
                    existingCategories.firstOrNull { it.name == name } ?: ProductCategory(name = name)
                },
                tags = jsonModel.getJSONArray("tags").toList().map { name ->
                    existingTags.firstOrNull { it.name == name } ?: ProductTag(name = name)
                }
            )
        }

        fun buildDefault(
            name: String,
            description: String,
        ): AIProductModel {
            return AIProductModel(
                names = listOf(name),
                descriptions = listOf(description),
                shortDescriptions = listOf(""),
                isVirtual = false,
                price = BigDecimal.ZERO,
                shipping = Shipping(0f, 0f, 0f, 0f)
            )
        }

        private fun JSONArray.toList(): List<String> {
            return (0 until length())
                .map { i -> getString(i) }
        }
    }
}
