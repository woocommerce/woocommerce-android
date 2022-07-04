package com.woocommerce.android.ui.products.variations.selector

import com.woocommerce.android.WooException
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class VariationSelectorRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    fun observeVariations(productId: Long): Flow<List<ProductVariation>> =
        productStore.observeVariations(selectedSite.get(), productId).map {
            it.map { variation -> variation.toAppModel() }
        }

    suspend fun getProduct(productId: Long) = withContext(Dispatchers.IO) {
        productStore.getProductByRemoteId(selectedSite.get(), productId)?.toAppModel()
    }

    suspend fun fetchVariations(
        productId: Long,
        offset: Int,
        pageSize: Int
    ): Result<Boolean> {
        return productStore.fetchProductVariations(selectedSite.get(), productId, offset, pageSize)
            .let { result ->
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.PRODUCTS,
                        "Fetching variations failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    Result.success(result.model!!)
                }
            }
    }
}
