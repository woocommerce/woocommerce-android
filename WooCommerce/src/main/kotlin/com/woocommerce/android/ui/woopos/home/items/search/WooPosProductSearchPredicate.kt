package com.woocommerce.android.ui.woopos.home.items.search

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.common.data.models.WooPosProductModel
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosProductSearchPredicate @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    private val whitespaceRegex = "\\s+".toRegex()
    private var cachedSupportsNameOrSkuSearch: Boolean? = null

    operator fun invoke(query: String): (WooPosProductModel) -> Boolean =
        when {
            query.isBlank() -> { _ -> true }
            isWooCoreSupportsNameOrSkuSearch() -> tokenizedSkuOrNameSearchPredicate(query)
            else -> simpleSearchPredicate(query)
        }

    private fun simpleSearchPredicate(query: String): (WooPosProductModel) -> Boolean {
        val terms: List<String> = query.split(whitespaceRegex).filter { it.isNotBlank() }.map { it.lowercase() }

        return { product ->
            val searchable = listOf(
                product.name,
                product.description,
                product.shortDescription
            ).joinToString(" ").lowercase()

            terms.all { term -> searchable.contains(term) }
        }
    }

    private fun tokenizedSkuOrNameSearchPredicate(query: String): (WooPosProductModel) -> Boolean {
        val tokens: List<String> = query
            .trim()
            .split(whitespaceRegex)
            .filter { it.isNotEmpty() }
            .map { it.lowercase() }

        return { product ->
            tokens.all { token ->
                product.name.contains(token, ignoreCase = true) || product.sku.contains(token, ignoreCase = true)
            }
        }
    }

    private fun isWooCoreSupportsNameOrSkuSearch(): Boolean {
        cachedSupportsNameOrSkuSearch?.let { return it }
        val wooCoreVersion = getWooCoreVersion() ?: return false
        val supportsNameOrSkuSearch =
            wooCoreVersion.semverCompareTo(WC_LAST_VERSION_WITHOUT_NAME_OR_SKU_SUPPORT) >= 0
        cachedSupportsNameOrSkuSearch = supportsNameOrSkuSearch
        return supportsNameOrSkuSearch
    }

    private companion object {
        const val WC_LAST_VERSION_WITHOUT_NAME_OR_SKU_SUPPORT = "9.9.0"
    }
}
