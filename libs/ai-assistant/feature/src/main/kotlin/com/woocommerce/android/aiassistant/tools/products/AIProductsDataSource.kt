package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import javax.inject.Inject

internal class AIProductsDataSource @Inject constructor(
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore,
) {
    data class ProductsPage(
        val products: List<WCProductModel>,
        val canLoadMore: Boolean,
    )

    data class ProductUpdate(
        val name: String? = null,
        val regularPrice: String? = null,
        val salePrice: String? = null,
        val stockQuantity: Int? = null,
        val status: String? = null,
    )

    data class BulkUpdateResult(
        val updatedIds: List<Long>,
        val failedProducts: List<WCProductStore.UpdateProductsResult.FailedProduct>,
    )

    class UnsupportedProductTypeException(productId: Long, productType: String) : IllegalArgumentException(
        "Product $productId has type '$productType'. This tool only updates simple products. " +
            "For variable products, update individual variations instead."
    )

    suspend fun fetchProducts(
        search: String? = null,
        status: String? = null,
        page: Int = 1,
        perPage: Int = PAGE_SIZE,
    ): Result<ProductsPage> {
        val site = selectedSite.get()
        val normalisedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val clampedPerPage = perPage.coerceIn(1, MAX_PAGE_SIZE)
        val offset = (page - 1) * clampedPerPage
        val filterOptions = buildMap<ProductFilterOption, String> {
            if (status != null && status != "any") put(ProductFilterOption.STATUS, status)
        }

        return if (normalisedSearch != null) {
            val result = productStore.searchProductsByNameAndSku(
                site = site,
                searchNameOrSkuQuery = normalisedSearch,
                offset = offset,
                pageSize = clampedPerPage,
                filterOptions = filterOptions,
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
                filterOptions = filterOptions,
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
        return getProduct(site, productId)
    }

    suspend fun getProducts(productIds: List<Long>): Result<List<WCProductModel>> {
        val ids = productIds.distinct()
        if (ids.isEmpty()) return Result.success(emptyList())

        val site = selectedSite.get()
        val result = productStore.fetchProducts(
            site = site,
            offset = 0,
            pageSize = ids.size,
            includedProductIds = ids,
            forceRefresh = false,
        )
        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            Result.success(ids.mapNotNull { id -> productStore.getProductByRemoteId(site, id) })
        }
    }

    suspend fun updateProduct(productId: Long, update: ProductUpdate): Result<WCProductModel> {
        val site = selectedSite.get()
        val existingProduct = getProduct(site, productId).getOrElse {
            return Result.failure(it)
        }
        if (existingProduct.type.isNotBlank() && existingProduct.type != SIMPLE_PRODUCT_TYPE) {
            return Result.failure(UnsupportedProductTypeException(productId, existingProduct.type))
        }

        val updatedProduct = existingProduct.copy(
            name = update.name ?: existingProduct.name,
            regularPrice = update.regularPrice ?: existingProduct.regularPrice,
            salePrice = update.salePrice ?: existingProduct.salePrice,
            stockQuantity = update.stockQuantity?.toDouble() ?: existingProduct.stockQuantity,
            manageStock = if (update.stockQuantity != null) true else existingProduct.manageStock,
            status = update.status ?: existingProduct.status,
        )
        val result = productStore.batchUpdateProducts(
            WCProductStore.BatchUpdateProductsPayload(
                site = site,
                updatedProducts = listOf(updatedProduct),
            )
        )

        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            Result.success(
                result.model?.firstOrNull { it.remoteProductId == productId } ?: updatedProduct
            )
        }
    }

    suspend fun bulkUpdateProducts(productIds: List<Long>, update: ProductUpdate): Result<BulkUpdateResult> {
        val site = selectedSite.get()
        val requests = productIds.associateWith {
            WCProductStore.UpdateProductRequest(
                name = update.name,
                regularPrice = update.regularPrice,
                salePrice = update.salePrice,
                stockQuantity = update.stockQuantity,
                status = update.status,
            )
        }
        val result = productStore.batchUpdateProducts(site, requests)

        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            val model = requireNotNull(result.model)
            Result.success(
                BulkUpdateResult(
                    updatedIds = model.updatedProducts,
                    failedProducts = model.failedProducts,
                )
            )
        }
    }

    private suspend fun getProduct(site: SiteModel, productId: Long): Result<WCProductModel> {
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
        private const val SIMPLE_PRODUCT_TYPE = "simple"
    }
}
