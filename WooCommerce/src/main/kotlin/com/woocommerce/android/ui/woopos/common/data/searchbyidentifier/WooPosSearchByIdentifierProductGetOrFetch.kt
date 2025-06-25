package com.woocommerce.android.ui.woopos.common.data.searchbyidentifier

import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.common.data.WooPosProductsCache
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class WooPosSearchByIdentifierProductGetOrFetch @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
    private val productsCache: WooPosProductsCache,
    private val errorMapper: WooPosSearchByIdentifierProductErrorMapper
) {
    suspend operator fun invoke(productId: Long): WooPosSearchByIdentifierResult {
        productsCache.getProductById(productId)?.let { cachedProduct ->
            return WooPosSearchByIdentifierResult.Success(cachedProduct)
        }

        val result = productStore.fetchSingleProduct(
            WCProductStore.FetchSingleProductPayload(
                site = selectedSite.get(),
                remoteProductId = productId,
            )
        )

        return when {
            result.isError -> WooPosSearchByIdentifierResult.Failure(errorMapper(result.error))

            else -> {
                val product = productStore.getProduct(selectedSite.get(), productId)?.toAppModel()
                if (product != null) {
                    productsCache.addAll(listOf(product))
                    WooPosSearchByIdentifierResult.Success(product)
                } else {
                    WooPosSearchByIdentifierResult.Failure(
                        WooPosSearchByIdentifierResult.Error.UnknownError(
                            "Product not found for ID: $productId"
                        )
                    )
                }
            }
        }
    }
}
