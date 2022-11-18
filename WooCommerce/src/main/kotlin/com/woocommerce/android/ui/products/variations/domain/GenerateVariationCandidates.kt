package com.woocommerce.android.ui.products.variations.domain

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductType

typealias VariationCandidate = List<VariantOption>

class GenerateVariationCandidates {

    operator fun invoke(
        product: Product,
        existingVariations: List<ProductVariation>
    ): List<VariationCandidate> {
        if (product.type != ProductType.VARIABLE.value) {
            return emptyList()
        }

        val termAssignmentsGroupedByAttribute: List<List<VariantOption>> = product.attributes
            .filter(ProductAttribute::isVariation)
            .map { productAttribute ->
                productAttribute.terms.map { term ->
                    VariantOption(
                        id = productAttribute.id,
                        name = productAttribute.name,
                        option = term,
                    )
                }
            }

        val existingVariationsAsCandidates: List<VariationCandidate> =
            existingVariations.map { it.attributes.toList() }

        val variationCandidates = cartesianProductForTermAssignments(
            termAssignmentsGroupedByAttribute
        ).minus(existingVariationsAsCandidates.toSet())

        return if (variationCandidates.first().isEmpty()) {
            emptyList()
        } else {
            variationCandidates
        }
    }

    private fun cartesianProductForTermAssignments(
        termsGroupedByAttribute: List<List<VariantOption>>
    ): List<VariationCandidate> = termsGroupedByAttribute.fold(
        listOf(emptyList())
    ) { acc: List<VariationCandidate>, assignmentsGroupedByAttribute: List<VariantOption> ->
        acc.flatMap { variationCandidate ->
            assignmentsGroupedByAttribute.map { termAssignment ->
                variationCandidate + termAssignment
            }
        }
    }
}
