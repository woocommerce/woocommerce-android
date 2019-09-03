package com.woocommerce.android.ui.products

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_LIST_LOAD_ERROR
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WCProductStore.ProductSorting
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class ProductListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
        private val PRODUCT_SORTING = ProductSorting.TITLE_ASC
    }

    private var continuation: Continuation<Boolean>? = null
    private var offset = 0
    private var isLoadingProducts = false

    final var canLoadMoreProducts = true
        private set(value) {
            field = value
        }

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProductList(loadMore: Boolean = false): List<Product> {
        if (!isLoadingProducts) {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
                continuation = it
                isLoadingProducts = true
                val payload = WCProductStore.FetchProductsPayload(
                        selectedSite.get(),
                        PRODUCT_PAGE_SIZE,
                        offset,
                        PRODUCT_SORTING
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        }

        return getProductList()
    }

    fun getProductList(): List<Product> {
        val wcProducts = productStore.getProductsForSite(selectedSite.get(), PRODUCT_SORTING)
        return wcProducts.map { it.toAppModel() }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            isLoadingProducts = false
            if (event.isError) {
                continuation?.resume(false)
                AnalyticsTracker.track(
                        PRODUCT_LIST_LOAD_ERROR,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                )
            } else {
                canLoadMoreProducts = event.canLoadMore
                AnalyticsTracker.track(PRODUCT_LIST_LOADED)
                continuation?.resume(true)
            }
        }
    }
}
