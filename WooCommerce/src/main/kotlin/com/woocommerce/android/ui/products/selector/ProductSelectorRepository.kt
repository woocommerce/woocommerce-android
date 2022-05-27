package com.woocommerce.android.ui.products.selector

import com.woocommerce.android.WooException
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class ProductSelectorRepository @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    suspend fun searchProducts(
        searchQuery: String,
        offset: Int,
        pageSize: Int
    ): Result<SearchResult> {
        return productStore.searchProducts(
            selectedSite.get(),
            searchString = searchQuery,
            offset = offset,
            pageSize = pageSize
        ).let { result ->
            if (result.isError) {
                WooLog.w(
                    WooLog.T.PRODUCTS,
                    "Searching products failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                val searchResult = result.model!!
                Result.success(
                    SearchResult(
                        products = searchResult.products.map { product -> product.toAppModel() },
                        canLoadMore = searchResult.canLoadMore
                    )
                )
            }
        }
    }

    fun observeProducts(): Flow<List<Product>> = productStore.observeProducts(selectedSite.get()).map {
        it.map { product -> product.toAppModel() }
    }

    suspend fun fetchProducts(
        offset: Int,
        pageSize: Int
    ): Result<Boolean> {
        return productStore.fetchProducts(selectedSite.get(), offset, pageSize)
            .let { result ->
                if (result.isError) {
                    WooLog.w(
                        WooLog.T.PRODUCTS,
                        "Fetching products failed, error: ${result.error.type}: ${result.error.message}"
                    )
                    Result.failure(WooException(result.error))
                } else {
                    Result.success(result.model!!)
                }
            }
    }

    data class SearchResult(
        val products: List<Product>,
        val canLoadMore: Boolean
    )
}
