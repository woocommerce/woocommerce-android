package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_LOAD_ERROR
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnProductsSearched
import org.wordpress.android.fluxc.store.WCProductStore.ProductFilterOption
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject
import kotlin.coroutines.resume

@OpenClassOnDebug
final class ProductListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
        private val PRODUCT_SORTING = ProductSorting.TITLE_ASC
    }

    private var loadContinuation: CancellableContinuation<Boolean>? = null
    private var searchContinuation: CancellableContinuation<List<Product>>? = null
    private var offset = 0

    final var canLoadMoreProducts = true
        private set

    final var lastSearchQuery: String? = null
        private set

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
        productFilterOptions: Map<ProductFilterOption, String>
    ): List<Product> {
        try {
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
                loadContinuation = it
                lastSearchQuery = null
                val payload = WCProductStore.FetchProductsPayload(
                        selectedSite.get(),
                        PRODUCT_PAGE_SIZE,
                        offset,
                        PRODUCT_SORTING,
                        filterOptions = productFilterOptions
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.d(WooLog.T.PRODUCTS, "CancellationException while fetching products")
        }

        return getProductList(productFilterOptions)
    }

    /**
     * Submits a fetch request to get a page of products for the current site matching the passed
     * query and returns only that page of products - note that this returns null if the search
     * is interrupted (which means the user submitted another search while this was running)
     */
    suspend fun searchProductList(searchQuery: String, loadMore: Boolean = false): List<Product>? {
        // cancel any existing load or search
        loadContinuation?.cancel()
        searchContinuation?.cancel()

        try {
            val products = suspendCancellableCoroutineWithTimeout<List<Product>>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
                searchContinuation = it
                lastSearchQuery = searchQuery
                val payload = WCProductStore.SearchProductsPayload(
                        selectedSite.get(),
                        searchQuery,
                        PRODUCT_PAGE_SIZE,
                        offset,
                        PRODUCT_SORTING
                )
                dispatcher.dispatch(WCProductActionBuilder.newSearchProductsAction(payload))
            }

            return products ?: emptyList()
        } catch (e: CancellationException) {
            WooLog.d(WooLog.T.PRODUCTS, "CancellationException while searching products")
            return null
        }
    }

    /**
     * Returns all products for the current site that are in the database
     */
    fun getProductList(productFilterOptions: Map<ProductFilterOption, String>): List<Product> {
        return if (selectedSite.exists()) {
            val wcProducts = productStore.getProductsByFilterOptions(
                    selectedSite.get(),
                    filterOptions = productFilterOptions,
                    sortType = PRODUCT_SORTING
            )
            wcProducts.map { it.toAppModel() }
        } else {
            WooLog.w(WooLog.T.PRODUCTS, "No site selected - unable to load products")
            emptyList()
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            if (event.isError) {
                loadContinuation?.resume(false)
                AnalyticsTracker.track(
                        PRODUCT_LIST_LOAD_ERROR,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                )
            } else {
                canLoadMoreProducts = event.canLoadMore
                AnalyticsTracker.track(PRODUCT_LIST_LOADED)
                loadContinuation?.resume(true)
            }
            loadContinuation = null
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductsSearched(event: OnProductsSearched) {
        if (event.isError) {
            searchContinuation?.resume(emptyList())
        } else {
            canLoadMoreProducts = event.canLoadMore
            val products = event.searchResults.map { it.toAppModel() }
            searchContinuation?.resume(products)
        }
        searchContinuation = null
    }
}
