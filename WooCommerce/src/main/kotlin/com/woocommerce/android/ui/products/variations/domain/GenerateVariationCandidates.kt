package com.woocommerce.android.ui.products.variations.domain

import com.woocommerce.android.model.Product
import com.woocommerce.android.ui.products.ProductType
import javax.inject.Inject

data class TermAssignment(val attributeName: String, val termName: String)
typealias VariationCandidate = List<TermAssignment>

class GenerateVariationCandidates @Inject constructor() {

    companion object {
        const val VARIATION_CREATION_LIMIT = 100
    }

    operator fun invoke(product: Product): List<VariationCandidate> {
        if (product.type != ProductType.VARIABLE.value) {
            return emptyList()
        }

        val termAssignmentsGroupedByAttribute: List<List<TermAssignment>> = product.attributes.map { productAttribute ->
            productAttribute.terms.map { term ->
                TermAssignment(
                    attributeName = productAttribute.name, termName = term
                )
            }
        }

        val variationCandidates = cartesianProductForTermAssignments(termAssignmentsGroupedByAttribute)

        return if (variationCandidates.first().isEmpty()) {
            emptyList()
        } else {
            variationCandidates
        }
    }

    private fun cartesianProductForTermAssignments(
        termsGroupedByAttribute: List<List<TermAssignment>>
    ): List<VariationCandidate> = termsGroupedByAttribute.fold(
        listOf(emptyList())
    ) { acc: List<VariationCandidate>, assignmentsGroupedByAttribute: List<TermAssignment> ->
        acc.flatMap { variationCandidate ->
            assignmentsGroupedByAttribute.map { termAssignment ->
                variationCandidate + termAssignment
            }
        }
    }
}
