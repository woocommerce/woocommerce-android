package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.model.Product
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductSearchPredicate @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    private val whitespaceRegex = "\\s+".toRegex()
    private var cachedSupportsNameOrSkuSearch: Boolean? = null

    operator fun invoke(query: String): (Product) -> Boolean =
        when {
            query.isBlank() -> { _ -> true }
            !isWooCoreSupportsNameOrSkuSearch() -> simpleSearchPredicate(query)
            else -> tokenizedSkuOrNameSearchPredicate(query)
        }

    private fun simpleSearchPredicate(query: String): (Product) -> Boolean {
        val terms: List<String> = query.split("\\s+".toRegex()).filter { it.isNotBlank() }.map { it.lowercase() }

        return { product ->
            if (terms.isEmpty()) true

            val searchable = listOf(
                product.name,
                product.description,
                product.shortDescription
            ).joinToString(" ").lowercase()

            terms.all { term -> searchable.contains(term) }
        }
    }

    private fun tokenizedSkuOrNameSearchPredicate(query: String): (Product) -> Boolean {
        val tokens: List<String> = query
            .trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }

        return { product ->
            tokens.all { token ->
                product.name.contains(token, ignoreCase = true) || product.sku.contains(token, ignoreCase = true)
            }
        }
    }

    @Suppress("ReturnCount")
    private fun isWooCoreSupportsNameOrSkuSearch(): Boolean {
        cachedSupportsNameOrSkuSearch?.let { return it }
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion >= WC_VERSION_SUPPORTS_NAME_OR_SKU_SEARCH
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_NAME_OR_SKU_SEARCH = "9.9.0"
    }
}
