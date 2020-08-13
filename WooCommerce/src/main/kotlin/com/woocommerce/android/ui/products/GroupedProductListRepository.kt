package com.woocommerce.android.ui.products

import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject
import kotlin.coroutines.resume

class GroupedProductListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
    }

    private var loadContinuation: CancellableContinuation<Boolean>? = null
    private var offset = 0

    var canLoadMoreProducts = true
        private set

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a page of products for the [groupedProductIds] and returns the full
     * list of products from the database
     */
    suspend fun fetchGroupedProductList(
        groupedProductIds: List<Long>,
        loadMore: Boolean = false
    ): List<Product> {
        try {
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
                loadContinuation = it
                val payload = FetchProductsPayload(
                    selectedSite.get(),
                    PRODUCT_PAGE_SIZE,
                    offset,
                    remoteProductIds = groupedProductIds
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.d(T.PRODUCTS, "CancellationException while fetching grouped products")
        }

        return getGroupedProductList(groupedProductIds)
    }

    /**
     * Returns all products for the [groupedProductIds] that are in the database
     */
    fun getGroupedProductList(groupedProductIds: List<Long>): List<Product> {
        return if (selectedSite.exists()) {
            val wcProducts = productStore.getProductsByRemoteIds(
                selectedSite.get(),
                remoteProductIds = groupedProductIds
            )
            wcProducts.map { it.toAppModel() }
        } else {
            WooLog.w(T.PRODUCTS, "No site selected - unable to load products")
            emptyList()
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCTS) {
            if (event.isError) {
                // TODO: add tracking event
                loadContinuation?.resume(false)
            } else {
                // TODO: add tracking event
                canLoadMoreProducts = event.canLoadMore
                loadContinuation?.resume(true)
            }
            loadContinuation = null
        }
    }
}
