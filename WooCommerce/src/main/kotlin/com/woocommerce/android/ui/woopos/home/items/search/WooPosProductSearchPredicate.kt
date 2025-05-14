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

    operator fun invoke(query: String): (Product) -> Boolean {
        if (query.isBlank()) return { true }

        val searchTerms = query.lowercase().split(whitespaceRegex).filter { it.isNotBlank() }

        return { product ->
            searchTerms.all { term ->
                if (!isWooCoreSupportsNameOrSkuSearch()) {
                    product.name.lowercase().contains(term)
                } else {
                    product.name.lowercase().contains(term) || product.sku.lowercase().contains(term)
                }
            }
        }
    }

    private fun isWooCoreSupportsNameOrSkuSearch(): Boolean {
        cachedSupportsNameOrSkuSearch?.let { return it }
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion >= WC_VERSION_SUPPORTS_NAME_OR_SKU_SEARCH
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_NAME_OR_SKU_SEARCH = "9.9.0"
    }
}
