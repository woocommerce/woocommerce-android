package com.woocommerce.android.ui.products.list

import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.RequestResult
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.products.ProductStatus
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel
import com.woocommerce.android.ui.products.UpdateProductStockStatusViewModel.UpdateStockStatusResult
import com.woocommerce.android.ui.subscriptions.IsEligibleForSubscriptions
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject

class ProductListRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val dispatchers: CoroutineDispatchers,
    private val isEligibleForSubscriptions: IsEligibleForSubscriptions
) {
    companion object {
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
    }

    private var searchContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)
    private var trashContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
    private var offset = 0

    var canLoadMoreProducts = true
        private set

    var lastSearchQuery: String? = null
        private set

    var lastIsSkuSearch = WCProductStore.SkuSearchOptions.Disabled
        private set

    var productSortingChoice: WCProductStore.ProductSorting
        get() {
            return WCProductStore.ProductSorting.valueOf(
                appPrefsWrapper.getProductSortingChoice(selectedSite.getSelectedSiteId())
                    ?: WCProductStore.ProductSorting.TITLE_ASC.name
            )
        }
        set(value) {
            appPrefsWrapper.setProductSortingChoice(selectedSite.getSelectedSiteId(), value.name)
        }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a page of products for the current site and returns the full
     * list of products from the database
     */
    suspend fun fetchProductList(
        loadMore: Boolean = false,
        productFilterOptions: Map<WCProductStore.ProductFilterOption, String> = emptyMap(),
        excludedProductIds: List<Long>? = null,
        sortType: WCProductStore.ProductSorting? = null
    ): Result<List<Product>> {
        offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
        lastSearchQuery = null
        lastIsSkuSearch = WCProductStore.SkuSearchOptions.Disabled

        return productStore.fetchProducts(
            site = selectedSite.get(),
            pageSize = PRODUCT_PAGE_SIZE,
            offset = offset,
            sortType = sortType ?: productSortingChoice,
            filterOptions = productFilterOptions,
            excludedProductIds = excludedProductIds.orEmpty()
        ).let { result ->
            if (result.isError) {
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_LOAD_ERROR,
                    this.javaClass.simpleName,
                    result.error.type.toString(),
                    result.error.message
                )
                WooLog.w(
                    WooLog.T.PRODUCTS,
                    "Fetching products failed, error: ${result.error.type}: ${result.error.message}"
                )
                Result.failure(WooException(result.error))
            } else {
                canLoadMoreProducts = result.model!!
                AnalyticsTracker.track(
                    AnalyticsEvent.PRODUCT_LIST_LOADED,
                    mapOf(AnalyticsTracker.KEY_IS_ELIGIBLE_FOR_SUBSCRIPTIONS to isEligibleForSubscriptions())
                )
                Result.success(getProductList(productFilterOptions, excludedProductIds))
            }
        }
    }

    /**
     * Submits a fetch request to get a page of products for the current site matching the passed
     * query and returns only that page of products - note that this returns null if the search
     * is interrupted (which means the user submitted another search while this was running)
     */
    suspend fun searchProductList(
        searchQuery: String,
        skuSearchOptions: WCProductStore.SkuSearchOptions,
        loadMore: Boolean = false,
        excludedProductIds: List<Long>? = null,
        productFilterOptions: Map<WCProductStore.ProductFilterOption, String> = emptyMap(),
    ): List<Product>? {
        val result = searchContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
            lastSearchQuery = searchQuery
            lastIsSkuSearch = skuSearchOptions
            val payload = WCProductStore.SearchProductsPayload(
                site = selectedSite.get(),
                searchQuery = searchQuery,
                skuSearchOptions = skuSearchOptions,
                pageSize = PRODUCT_PAGE_SIZE,
                offset = offset,
                sorting = productSortingChoice,
                excludedProductIds = excludedProductIds,
                filterOptions = productFilterOptions
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation -> null
            is ContinuationWrapper.ContinuationResult.Success -> result.value
        }
    }

    /**
     * Dispatches a request to trash a specific product
     */
    suspend fun trashProduct(remoteProductId: Long): Boolean {
        val result = trashContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.DeleteProductPayload(
                selectedSite.get(),
                remoteProductId,
                forceDelete = false
            )
            dispatcher.dispatch(WCProductActionBuilder.newDeleteProductAction(payload))
        }

        return when (result) {
            is ContinuationWrapper.ContinuationResult.Cancellation -> false
            is ContinuationWrapper.ContinuationResult.Success -> result.value
        }
    }

    /**
     * Returns all products for the current site that are in the database
     */
    fun getProductList(
        productFilterOptions: Map<WCProductStore.ProductFilterOption, String> = emptyMap(),
        excludedProductIds: List<Long>? = null,
        sortType: WCProductStore.ProductSorting? = null
    ): List<Product> {
        val excludedIds = excludedProductIds?.takeIf { it.isNotEmpty() }
        return if (selectedSite.exists()) {
            val wcProducts = productStore.getProducts(
                selectedSite.get(),
                filterOptions = productFilterOptions,
                sortType = sortType ?: productSortingChoice,
                excludedProductIds = excludedIds
            )
            wcProducts.map { it.toAppModel() }
        } else {
            WooLog.w(WooLog.T.PRODUCTS, "No site selected - unable to load products")
            emptyList()
        }
    }

    fun observeProducts(
        filterOptions: Map<WCProductStore.ProductFilterOption, String>,
        sortType: WCProductStore.ProductSorting? = null,
        excludeSampleProducts: Boolean = false,
        limit: Int? = null
    ): Flow<List<Product>> = productStore.observeProducts(
        selectedSite.get(),
        filterOptions = filterOptions,
        sortType = sortType ?: productSortingChoice,
        excludeSampleProducts = excludeSampleProducts,
        limit = limit
    ).map {
        it.map { product -> product.toAppModel() }
    }

    fun observeProductsCount(
        filterOptions: Map<WCProductStore.ProductFilterOption, String>,
        excludeSampleProducts: Boolean = false
    ): Flow<Long> = productStore.observeProductsCount(
        selectedSite.get(),
        filterOptions = filterOptions,
        excludeSampleProducts = excludeSampleProducts
    )

    /**
     * Returns a single product
     */
    fun getProduct(remoteProductId: Long) = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: WCProductStore.OnProductChanged) {
        if (event.causeOfChange == WCProductAction.DELETED_PRODUCT) {
            if (event.isError) {
                trashContinuation.continueWith(false)
            } else {
                trashContinuation.continueWith(true)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: WCProductStore.OnProductsSearched) {
        if (event.isError) {
            searchContinuation.continueWith(emptyList())
        } else {
            canLoadMoreProducts = event.canLoadMore
            val products = event.searchResults.map { it.toAppModel() }
            searchContinuation.continueWith(products)
        }
    }

    suspend fun bulkUpdateProductsStatus(
        productsIds: Collection<Long>,
        newStatus: ProductStatus,
    ): RequestResult = withContext(dispatchers.io) {
        val updatedProducts = productStore.getProductsByRemoteIds(
            site = selectedSite.get(),
            remoteProductIds = productsIds.toList()
        ).map {
            it.apply {
                status = newStatus.toString()
            }
        }

        bulkUpdateProducts(updatedProducts)
    }

    suspend fun bulkUpdateProductsPrice(
        productsIds: List<Long>,
        newRegularPrice: String,
    ): RequestResult = withContext(dispatchers.io) {
        val updatedProducts = productStore.getProductsByRemoteIds(
            site = selectedSite.get(),
            remoteProductIds = productsIds
        ).map {
            it.apply {
                regularPrice = newRegularPrice
            }
        }

        bulkUpdateProducts(updatedProducts)
    }

    private suspend fun bulkUpdateProducts(updatedProducts: List<WCProductModel>) =
        productStore.batchUpdateProducts(
            WCProductStore.BatchUpdateProductsPayload(
                selectedSite.get(),
                updatedProducts
            )
        ).let {
            if (it.isError) {
                RequestResult.ERROR
            } else {
                RequestResult.SUCCESS
            }
        }

    suspend fun fetchStockStatuses(
        productIds: List<Long>
    ): List<UpdateProductStockStatusViewModel.ProductStockStatusInfo> =
        withContext(dispatchers.io) {
            productStore.getProductsByRemoteIds(selectedSite.get(), productIds).map { wcProductModel ->
                UpdateProductStockStatusViewModel.ProductStockStatusInfo(
                    productId = wcProductModel.remoteProductId,
                    stockStatus = ProductStockStatus.fromString(wcProductModel.stockStatus),
                    manageStock = wcProductModel.manageStock,
                    isVariable = ProductType.fromString(wcProductModel.type).isVariableProduct()
                )
            }
        }

    suspend fun bulkUpdateStockStatus(productIds: List<Long>, newStatus: ProductStockStatus): UpdateStockStatusResult =
        withContext(dispatchers.io) {
            val allProducts = productStore.getProductsByRemoteIds(selectedSite.get(), productIds)
            val variableProductsCount = allProducts.count { ProductType.fromString(it.type).isVariableProduct() }

            if (variableProductsCount == allProducts.size) {
                return@withContext UpdateStockStatusResult.IsVariableProducts
            }

            val productsToUpdate =
                allProducts.filterNot { it.manageStock || ProductType.fromString(it.type).isVariableProduct() }.map {
                    it.apply { stockStatus = ProductStockStatus.fromStockStatus(newStatus) }
                }

            if (productsToUpdate.isEmpty() && allProducts.isNotEmpty()) {
                return@withContext UpdateStockStatusResult.IsManagedProducts
            }

            return@withContext when (bulkUpdateProducts(productsToUpdate)) {
                RequestResult.SUCCESS -> UpdateStockStatusResult.Updated
                else -> UpdateStockStatusResult.Error
            }
        }
}
