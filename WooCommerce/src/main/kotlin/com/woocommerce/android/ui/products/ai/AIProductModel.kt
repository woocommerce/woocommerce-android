package com.woocommerce.android.ui.products.ai

import com.google.gson.annotations.SerializedName
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.ProductTag
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductStatus.DRAFT
import com.woocommerce.android.ui.products.ProductType.SIMPLE
import java.math.BigDecimal

data class AIProductModel(
    val names: List<String>,
    val descriptions: List<String>,
    @SerializedName("short_descriptions") val shortDescriptions: List<String>,
    @SerializedName("virtual") val isVirtual: Boolean,
    val price: BigDecimal,
    val shipping: Shipping,
    val categories: List<String>? = null,
    val tags: List<String>? = null
) {
    @Suppress("unused")
    fun toProduct(
        variant: Int,
        existingCategories: List<ProductCategory>,
        existingTags: List<ProductTag>
    ): Product = ProductHelper.getDefaultNewProduct(SIMPLE, isVirtual).copy(
        name = names[variant],
        description = descriptions[variant],
        shortDescription = shortDescriptions[variant],
        regularPrice = price,
        categories = getCategories(existingCategories),
        tags = getTags(existingTags),
        weight = shipping.weight,
        height = shipping.height,
        length = shipping.length,
        width = shipping.width,
        status = DRAFT
    )

    private fun getCategories(
        existingCategories: List<ProductCategory>
    ): List<ProductCategory> = categories.orEmpty().map { name ->
        existingCategories.find { category ->
            category.name == name
        } ?: ProductCategory(name = name)
    }

    private fun getTags(
        existingTags: List<ProductTag>
    ): List<ProductTag> = tags.orEmpty().map { name ->
        existingTags.find { tag ->
            tag.name == name
        } ?: ProductTag(name = name)
    }

    data class Shipping(
        val weight: Float,
        val height: Float,
        val length: Float,
        val width: Float
    )

    companion object {
        @Suppress("UNUSED_PARAMETER")
        fun fromJson(json: String): AIProductModel {
            TODO()
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
    }
}
