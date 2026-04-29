package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

internal class AIProductsDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
) {
    data class ProductsPage(
        val products: List<WCProductModel>,
        val canLoadMore: Boolean,
    )

    suspend fun fetchProducts(
        search: String? = null,
        page: Int = 1,
        perPage: Int = PAGE_SIZE,
    ): Result<ProductsPage> {
        val site = selectedSite.get()
        val normalisedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val clampedPerPage = perPage.coerceIn(1, MAX_PAGE_SIZE)
        val offset = (page - 1) * clampedPerPage

        return if (normalisedSearch != null) {
            val result = productStore.searchProductsByNameAndSku(
                site = site,
                searchNameOrSkuQuery = normalisedSearch,
                offset = offset,
                pageSize = clampedPerPage,
            )
            if (result.isError) {
                Result.failure(OnChangedException(requireNotNull(result.error)))
            } else {
                val searchResult = requireNotNull(result.model)
                Result.success(ProductsPage(products = searchResult.products, canLoadMore = searchResult.canLoadMore))
            }
        } else {
            val result = productStore.fetchProducts(
                site = site,
                offset = offset,
                pageSize = clampedPerPage,
            )
            if (result.isError) {
                Result.failure(OnChangedException(requireNotNull(result.error)))
            } else {
                val products = requireNotNull(result.model)
                Result.success(ProductsPage(products = products, canLoadMore = products.size >= clampedPerPage))
            }
        }
    }

    suspend fun getProduct(productId: Long): Result<WCProductModel> {
        val site = selectedSite.get()
        val cached = productStore.getProductByRemoteId(site, productId)
        if (cached != null) return Result.success(cached)

        val payload = WCProductStore.FetchSingleProductPayload(site, productId)
        val event = productStore.fetchSingleProduct(payload)
        return if (event.isError) {
            Result.failure(OnChangedException(requireNotNull(event.error)))
        } else {
            val product = productStore.getProductByRemoteId(site, productId)
            product?.let { Result.success(it) }
                ?: Result.failure(IllegalStateException("Product $productId not found after fetch"))
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}
