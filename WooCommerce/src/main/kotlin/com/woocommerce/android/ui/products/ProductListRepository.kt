package com.woocommerce.android.ui.products

import com.woocommerce.android.AppConstants
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_LOADED
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_LIST_LOAD_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.DELETED_PRODUCT
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductsSearched
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting.TITLE_ASC
import javax.inject.Inject

class ProductListRepository @Inject constructor(
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
    }

    private var loadContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
    private var searchContinuation = ContinuationWrapper<List<Product>>(WooLog.T.PRODUCTS)
    private var trashContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
    private var offset = 0

    var canLoadMoreProducts = true
        private set

    var lastSearchQuery: String? = null
        private set

    var productSortingChoice: ProductSorting
        get() {
            return ProductSorting.valueOf(
                appPrefsWrapper.getProductSortingChoice(selectedSite.getSelectedSiteId()) ?: TITLE_ASC.name
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
        productFilterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludedProductIds: List<Long>? = null
    ): List<Product> {
        loadContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
            lastSearchQuery = null
            val payload = WCProductStore.FetchProductsPayload(
                selectedSite.get(),
                PRODUCT_PAGE_SIZE,
                offset,
                productSortingChoice,
                filterOptions = productFilterOptions,
                excludedProductIds = excludedProductIds
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
        }

        return getProductList(productFilterOptions, excludedProductIds)
    }

    /**
     * Submits a fetch request to get a page of products for the current site matching the passed
     * query and returns only that page of products - note that this returns null if the search
     * is interrupted (which means the user submitted another search while this was running)
     */
    suspend fun searchProductList(
        searchQuery: String,
        loadMore: Boolean = false,
        excludedProductIds: List<Long>? = null
    ): List<Product>? {
        // cancel any existing load
        loadContinuation.cancel()

        val result = searchContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
            lastSearchQuery = searchQuery
            val payload = WCProductStore.SearchProductsPayload(
                selectedSite.get(),
                searchQuery,
                PRODUCT_PAGE_SIZE,
                offset,
                productSortingChoice,
                excludedProductIds = excludedProductIds
            )
            dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
        }

        return when (result) {
            is Cancellation -> null
            is Success -> result.value
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
            is Cancellation -> false
            is Success -> result.value
        }
    }

    /**
     * Returns all products for the current site that are in the database
     */
    fun getProductList(
        productFilterOptions: Map<ProductFilterOption, String> = emptyMap(),
        excludedProductIds: List<Long>? = null
    ): List<Product> {
        val excludedIds = excludedProductIds?.takeIf { it.isNotEmpty() }
        return if (selectedSite.exists()) {
            val wcProducts = productStore.getProductsByFilterOptions(
                selectedSite.get(),
                filterOptions = productFilterOptions,
                sortType = productSortingChoice,
                excludedProductIds = excludedIds
            )
            wcProducts.map { it.toAppModel() }
        } else {
            WooLog.w(WooLog.T.PRODUCTS, "No site selected - unable to load products")
            emptyList()
        }
    }

    /**
     * Returns a single product
     */
    fun getProduct(remoteProductId: Long) = productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            if (event.isError) {
                loadContinuation.continueWith(false)
                AnalyticsTracker.track(
                    PRODUCT_LIST_LOAD_ERROR,
                    this.javaClass.simpleName,
                    event.error.type.toString(),
                    event.error.message
                )
            } else {
                canLoadMoreProducts = event.canLoadMore
                AnalyticsTracker.track(PRODUCT_LIST_LOADED)
                loadContinuation.continueWith(true)
            }
        } else if (event.causeOfChange == DELETED_PRODUCT) {
            if (event.isError) {
                trashContinuation.continueWith(false)
            } else {
                trashContinuation.continueWith(true)
            }
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: OnProductsSearched) {
        if (event.isError) {
            searchContinuation.continueWith(emptyList())
        } else {
            canLoadMoreProducts = event.canLoadMore
            val products = event.searchResults.map { it.toAppModel() }
            searchContinuation.continueWith(products)
        }
    }
}
