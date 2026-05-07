package com.woocommerce.android.aiassistant.tools.products

import com.woocommerce.android.OnChangedException
import com.woocommerce.android.aiassistant.tools.CachedLookupResult
import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.SkuSearchOptions
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

    class ProductNotFoundException(productId: Long) : NoSuchElementException("Product $productId not found")

    suspend fun fetchProducts(
        search: String? = null,
        status: String? = null,
        page: Int = 1,
        perPage: Int = PAGE_SIZE,
        category: Int? = null,
        sku: String? = null,
        include: List<Long>? = null,
        stockStatus: String? = null,
        orderby: String? = null,
        order: String? = null,
    ): Result<ProductsPage> {
        val site = selectedSite.get()
        val normalisedSearch = search?.trim()?.takeIf { it.isNotEmpty() }
        val normalisedSku = sku?.trim()?.takeIf { it.isNotEmpty() }
        val normalisedInclude = include?.distinct()?.takeIf { it.isNotEmpty() }
        val clampedPerPage = perPage.coerceIn(1, MAX_PAGE_SIZE)
        val offset = (page - 1) * clampedPerPage
        val sortType = resolveSortType(orderby, order)
        val filterOptions = productFilterOptions(status, category, stockStatus)
        val fetchOptions = ProductFetchOptions(sortType = sortType, filterOptions = filterOptions)

        return when {
            normalisedSku != null -> fetchProductsBySku(site, normalisedSku, offset, clampedPerPage, filterOptions)
            normalisedSearch != null -> searchProducts(site, normalisedSearch, offset, clampedPerPage, filterOptions)
            normalisedInclude != null -> fetchIncludedProducts(
                site = site,
                include = normalisedInclude,
                offset = offset,
                pageSize = clampedPerPage,
                fetchOptions = fetchOptions,
            )
            else -> fetchProductPage(site, offset, clampedPerPage, sortType, filterOptions)
        }
    }

    private data class ProductFetchOptions(
        val sortType: ProductSorting,
        val filterOptions: Map<ProductFilterOption, String>,
    )

    private fun productFilterOptions(
        status: String?,
        category: Int?,
        stockStatus: String?,
    ): Map<ProductFilterOption, String> = buildMap {
        if (status != null && status != "any") put(ProductFilterOption.STATUS, status)
        category?.let { put(ProductFilterOption.CATEGORY, it.toString()) }
        stockStatus?.let { put(ProductFilterOption.STOCK_STATUS, it) }
    }

    private suspend fun fetchProductsBySku(
        site: SiteModel,
        sku: String,
        offset: Int,
        pageSize: Int,
        filterOptions: Map<ProductFilterOption, String>,
    ): Result<ProductsPage> {
        val result = productStore.searchProducts(
            site = site,
            searchString = sku,
            skuSearchOptions = SkuSearchOptions.ExactSearch,
            offset = offset,
            pageSize = pageSize,
            filterOptions = filterOptions,
        )
        return result.toProductsPage()
    }

    private suspend fun searchProducts(
        site: SiteModel,
        search: String,
        offset: Int,
        pageSize: Int,
        filterOptions: Map<ProductFilterOption, String>,
    ): Result<ProductsPage> {
        val result = productStore.searchProductsByNameAndSku(
            site = site,
            searchNameOrSkuQuery = search,
            offset = offset,
            pageSize = pageSize,
            filterOptions = filterOptions,
        )
        return result.toProductsPage()
    }

    private suspend fun fetchIncludedProducts(
        site: SiteModel,
        include: List<Long>,
        offset: Int,
        pageSize: Int,
        fetchOptions: ProductFetchOptions,
    ): Result<ProductsPage> {
        val pagedInclude = include.drop(offset).take(pageSize)
        if (pagedInclude.isEmpty()) {
            return Result.success(ProductsPage(products = emptyList(), canLoadMore = false))
        }

        val result = productStore.fetchProducts(
            site = site,
            offset = 0,
            pageSize = pagedInclude.size.coerceIn(1, MAX_PAGE_SIZE),
            sortType = fetchOptions.sortType,
            includedProductIds = pagedInclude,
            filterOptions = fetchOptions.filterOptions,
            forceRefresh = false,
        )
        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            Result.success(
                ProductsPage(
                    products = productStore.getProductsByRemoteIds(site, pagedInclude),
                    canLoadMore = include.size > offset + pageSize,
                )
            )
        }
    }

    private suspend fun fetchProductPage(
        site: SiteModel,
        offset: Int,
        pageSize: Int,
        sortType: ProductSorting,
        filterOptions: Map<ProductFilterOption, String>,
    ): Result<ProductsPage> {
        val result = productStore.fetchProducts(
            site = site,
            offset = offset,
            pageSize = pageSize,
            sortType = sortType,
            filterOptions = filterOptions,
        )
        return if (result.isError) {
            Result.failure(OnChangedException(requireNotNull(result.error)))
        } else {
            val products = requireNotNull(result.model)
            Result.success(ProductsPage(products = products, canLoadMore = products.size >= pageSize))
        }
    }

    private fun WooResult<WCProductStore.ProductSearchResult>.toProductsPage(): Result<ProductsPage> =
        if (isError) {
            Result.failure(OnChangedException(requireNotNull(error)))
        } else {
            val searchResult = requireNotNull(model)
            Result.success(ProductsPage(products = searchResult.products, canLoadMore = searchResult.canLoadMore))
        }

    private fun resolveSortType(orderby: String?, order: String?): ProductSorting =
        when (orderby) {
            "date" -> if (order == "asc") ProductSorting.DATE_ASC else ProductSorting.DATE_DESC
            "popularity" -> if (order == "asc") ProductSorting.POPULARITY_ASC else ProductSorting.POPULARITY_DESC
            else -> if (order == "desc") ProductSorting.TITLE_DESC else ProductSorting.TITLE_ASC
        }

    suspend fun getProduct(productId: Long): Result<WCProductModel> {
        val site = selectedSite.get()
        return getProduct(site, productId)
    }

    suspend fun getProducts(productIds: List<Long>): Result<CachedLookupResult<WCProductModel>> {
        val ids = productIds.distinct()
        if (ids.isEmpty()) {
            return Result.success(
                CachedLookupResult(
                    items = emptyList(),
                    cacheHitCount = 0,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        }

        val site = selectedSite.get()
        val cachedProducts = productStore.getProductsByRemoteIds(site, ids)
        val cachedIds = cachedProducts.map { it.remoteProductId }.toSet()
        val idsToFetch = ids.filterNot { it in cachedIds }
        if (idsToFetch.isEmpty()) {
            return Result.success(
                CachedLookupResult(
                    items = cachedProducts,
                    cacheHitCount = cachedIds.size,
                    cacheMissCount = 0,
                    fetchAttempted = false,
                    fetchFailed = false,
                )
            )
        }

        val result = productStore.fetchProducts(
            site = site,
            offset = 0,
            pageSize = idsToFetch.size,
            includedProductIds = idsToFetch,
            forceRefresh = false,
        )
        return if (result.isError) {
            Result.success(
                CachedLookupResult(
                    items = cachedProducts,
                    cacheHitCount = cachedIds.size,
                    cacheMissCount = idsToFetch.size,
                    fetchAttempted = true,
                    fetchFailed = true,
                )
            )
        } else {
            Result.success(
                CachedLookupResult(
                    items = productStore.getProductsByRemoteIds(site, ids),
                    cacheHitCount = cachedIds.size,
                    cacheMissCount = idsToFetch.size,
                    fetchAttempted = true,
                    fetchFailed = false,
                )
            )
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
                ?: Result.failure(ProductNotFoundException(productId))
        }
    }

    companion object {
        private const val PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
        private const val SIMPLE_PRODUCT_TYPE = "simple"
    }
}
