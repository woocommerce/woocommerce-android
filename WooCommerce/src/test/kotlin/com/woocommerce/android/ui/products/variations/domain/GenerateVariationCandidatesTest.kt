package com.woocommerce.android.ui.products.variations.domain

import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductHelper
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.generateVariation
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class GenerateVariationCandidatesTest {

    val sut = GenerateVariationCandidates()

    @Test
    fun `should generate all variation candidates for a product with 3 attributes each with 2 terms`() {
        // given
        val product = ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, isVirtual = false).copy(
            attributes = listOf(
                ProductAttribute(id = 1, name = "Size", terms = listOf("S", "M"), isVariation = true),
                ProductAttribute(id = 2, name = "Color", terms = listOf("Red", "Blue"), isVariation = true),
                ProductAttribute(id = 2, name = "Type", terms = listOf("Sport", "Casual"), isVariation = true)
            )
        )

        val expectedVariationCandidates = listOf(
            Triple("M", "Blue", "Sport"),
            Triple("M", "Blue", "Casual"),
            Triple("S", "Blue", "Sport"),
            Triple("S", "Blue", "Casual"),
            Triple("M", "Red", "Sport"),
            Triple("M", "Red", "Casual"),
            Triple("S", "Red", "Sport"),
            Triple("S", "Red", "Casual"),
        ).map { triple ->
            listOf(
                VariantOption(1, "Size", triple.first),
                VariantOption(2, "Color", triple.second),
                VariantOption(3, "Type", triple.third)
            )
        }

        // when
        val result = sut.invoke(product, emptyList())

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedVariationCandidates)
    }

    @Test
    fun `should generate empty list of candidates if product has no attributes`() {
        // given
        val product =
            ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, isVirtual = false).copy(attributes = emptyList())

        // when
        val result = sut.invoke(product, emptyList())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should generate empty list of candidates if product is not type of variable`() {
        // given
        val product =
            ProductHelper.getDefaultNewProduct(
                ProductType.SIMPLE,
                isVirtual = false
            ).copy(attributes = ProductTestUtils.generateProductAttributeList())

        // when
        val result = sut.invoke(product, emptyList())

        // then
        assertThat(result).isEmpty()
    }

    @Test
    fun `should not generate variation candidates with an attribute that is not a variable one`() {
        // given
        val product = ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, isVirtual = false).copy(
            attributes = listOf(
                ProductAttribute(id = 1, name = "Size", terms = listOf("S", "M"), isVariation = true),
                ProductAttribute(id = 2, name = "Color", terms = listOf("Red", "Blue"), isVariation = true),
                ProductAttribute(id = 3, name = "Type", terms = listOf("Sport", "Casual"), isVariation = false)
            )
        )

        val expectedVariationCandidates = listOf(
            Pair("M", "Blue"),
            Pair("S", "Blue"),
            Pair("M", "Red"),
            Pair("S", "Red"),
        ).map { triple ->
            listOf(
                VariantOption(1, "Size", triple.first),
                VariantOption(2, "Color", triple.second),
            )
        }

        // when
        val result = sut.invoke(product, emptyList())

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(expectedVariationCandidates)
    }

    @Test
    fun `should not generate variation candidate if variation already exists`() {
        // given
        val product = ProductHelper.getDefaultNewProduct(ProductType.VARIABLE, isVirtual = false).copy(
            attributes = listOf(
                ProductAttribute(id = 1, name = "Size", terms = listOf("S", "M"), isVariation = true),
                ProductAttribute(id = 2, name = "Color", terms = listOf("Red", "Blue"), isVariation = true),
                ProductAttribute(id = 3, name = "Type", terms = listOf("Sport", "Casual"), isVariation = true)
            )
        )

        val existingVariations: List<ProductVariation> = listOf(
            Triple("M", "Blue", "Sport"),
            Triple("M", "Blue", "Casual"),
            Triple("S", "Red", "Sport"),
            Triple("S", "Red", "Casual"),
        ).map { triple ->
            generateVariation().copy(
                attributes =
                arrayOf(
                    VariantOption(1, "Size", triple.first),
                    VariantOption(2, "Color", triple.second),
                    VariantOption(3, "Type", triple.third)
                )
            )
        }

        // when
        val result = sut.invoke(product, existingVariations)

        // then
        assertThat(result).doesNotContainAnyElementsOf(existingVariations.map { it.attributes.toList() })
    }
}
