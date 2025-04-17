package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductSearchPredicate @Inject constructor() {
    private val whitespaceRegex = "\\s+".toRegex()

    operator fun invoke(query: String): (Product) -> Boolean {
        if (query.isBlank()) return { true }

        val searchTerms = query.lowercase().split(whitespaceRegex).filter { it.isNotBlank() }

        return { product ->
            searchTerms.all { term ->
                product.name.lowercase().contains(term) ||
                    product.description.lowercase().contains(term) == true ||
                    product.shortDescription.lowercase().contains(term) == true
            }
        }
    }
}
