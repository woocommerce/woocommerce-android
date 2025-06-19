package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.util.WooPosLogWrapper
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierGlobalUniqueSearch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val wooPosLogWrapper: WooPosLogWrapper,
) {
    suspend operator fun invoke(globalUniqueId: String): WooPosSearchByIdentifierResult {
        val result = productStore.searchProducts(
            site = selectedSite.get(),
            searchString = null,
            globalUniqueIdSearchQuery = globalUniqueId,
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
                        WooPosSearchByIdentifierResult.Error.NotFound
                    )

                    else -> WooPosSearchByIdentifierResult.Success(products.first())
                }
            }

            else -> {
                wooPosLogWrapper.e("Result.isError == false but the model is missing.")
                WooPosSearchByIdentifierResult.Failure(
                    WooPosSearchByIdentifierResult.Error.UnknownError(
                        "Results not found for Global Unique ID: $globalUniqueId"
                    )
                )
            }
        }
    }
}
