package com.woocommerce.android.ui.products.variations.domain

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductAttribute
import com.woocommerce.android.model.VariantOption
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.variations.VariationRepository
import javax.inject.Inject

typealias VariationCandidate = List<VariantOption>

class GenerateVariationCandidates @Inject constructor(
    private val variationRepository: VariationRepository
) {

    companion object {
        const val VARIATION_CREATION_LIMIT = 100

        private val initialAccumulator
            get() = listOf(emptyList<VariationCandidate>())
    }

    suspend operator fun invoke(product: Product): List<VariationCandidate> {
        if (!ProductType.fromString(product.type).isVariableProduct()) {
            return emptyList()
        }

        val existingVariations = variationRepository.getAllVariations(product.remoteId)

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

        return if (variationCandidates == initialAccumulator) {
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
