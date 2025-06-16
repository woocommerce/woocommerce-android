package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierGtinSearch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    suspend operator fun invoke(gtin: String): Result<List<Product>> {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = null,
            globalUniqueIdSearchQuery = gtin,
            offset = 0,
            pageSize = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE,
            filterOptions = emptyMap()
        )

        return when {
            result.isError -> Result.failure(WooPosSearchByIdentifierException(WooPosSearchByIdentifierResult.Error.NetworkError))
            result.model != null -> {
                val productSearchResult = result.model!!
                Result.success(productSearchResult.products.map { it.toAppModel() })
            }
            else -> Result.failure(WooPosSearchByIdentifierException(WooPosSearchByIdentifierResult.Error.RequestCancelled))
        }
    }
}
