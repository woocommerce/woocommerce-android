package com.woocommerce.android.ui.products.categories

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.ProductCategory
import com.woocommerce.android.model.toProductCategory
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_CATEGORIES
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductCategoryChanged
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class ProductCategoriesRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_CATEGORIES_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_CATEGORY_PAGE_SIZE
    }

    private var loadContinuation: Continuation<Boolean>? = null
    private var offset = 0

    var canLoadMoreProductCategories = true

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a list of products categories for the current site
     * and returns the full list of product categories from the database
     */
    suspend fun fetchProductCategories(loadMore: Boolean = false): List<ProductCategory> {
        try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_CATEGORIES_PAGE_SIZE else 0
                loadContinuation = it
                val payload = WCProductStore.FetchProductCategoriesPayload(
                    selectedSite.get(),
                    pageSize = PRODUCT_CATEGORIES_PAGE_SIZE,
                    offset = offset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductCategoriesAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(WooLog.T.PRODUCTS, "CancellationException while fetching product categories", e)
        }

        return getProductCategoriesList()
    }

    /**
     * Returns all product categories for the current site that are in the database
     */
    fun getProductCategoriesList(): List<ProductCategory> {
        return productStore.getProductCategoriesForSite(selectedSite.get())
            .map { it.toProductCategory() }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductCategoriesChanged(event: OnProductCategoryChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_CATEGORIES) {
            if (event.isError) {
                loadContinuation?.resume(false)
                AnalyticsTracker.track(
                    Stat.PRODUCT_CATEGORIES_LOAD_FAILED,
                    this.javaClass.simpleName,
                    event.error.type.toString(),
                    event.error.message
                )
            } else {
                canLoadMoreProductCategories = event.canLoadMore
                AnalyticsTracker.track(Stat.PRODUCT_CATEGORIES_LOADED)
                loadContinuation?.resume(true)
            }
            loadContinuation = null
        }
    }
}
