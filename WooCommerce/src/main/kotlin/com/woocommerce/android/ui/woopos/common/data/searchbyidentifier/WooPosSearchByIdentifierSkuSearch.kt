package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierSkuSearch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    suspend operator fun invoke(sku: String): WooPosSearchByIdentifierResult {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = sku,
            skuSearchOptions = WCProductStore.SkuSearchOptions.ExactSearch,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            filterOptions = emptyMap()
        )

        return when {
            result.isError -> WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.NetworkError
            )

            result.model != null -> {
                val productSearchResult = result.model!!
                val products = productSearchResult.products.map { it.toAppModel() }
                when {
                    products.isEmpty() -> WooPosSearchByIdentifierResult.Failure(
                        WooPosSearchByIdentifierResult.Error.ProductNotFound
                    )
                    else -> WooPosSearchByIdentifierResult.Success(products.first())
                }
            }

            else -> WooPosSearchByIdentifierResult.Failure(
                WooPosSearchByIdentifierResult.Error.UnknownError("Results not found for SKU: $sku")
            )
        }
    }
}
