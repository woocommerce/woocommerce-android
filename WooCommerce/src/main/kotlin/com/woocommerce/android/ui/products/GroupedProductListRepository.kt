package com.woocommerce.android.ui.products

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCTS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductsPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import javax.inject.Inject

class GroupedProductListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val selectedSite: SelectedSite,
    private val productStore: WCProductStore
) {
    companion object {
        private const val PRODUCT_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_PAGE_SIZE
    }

    private var loadContinuation = ContinuationWrapper<Boolean>(T.PRODUCTS)
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
     * Submits a fetch request to get a page of products for the [productIds] and returns the full
     * list of products from the database
     */
    suspend fun fetchProductList(
        productIds: List<Long>,
        loadMore: Boolean = false
    ): List<Product> {
        loadContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_PAGE_SIZE else 0
            val payload = FetchProductsPayload(
                selectedSite.get(),
                PRODUCT_PAGE_SIZE,
                offset,
                remoteProductIds = productIds
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductsAction(payload))
        }

        return getProductList(productIds)
    }

    /**
     * Returns all products for the [productIds] that are in the database
     */
    fun getProductList(productIds: List<Long>): List<Product> {
        return if (selectedSite.exists()) {
            val wcProducts = productStore.getProductsByRemoteIds(
                selectedSite.get(),
                remoteProductIds = productIds
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
                loadContinuation.continueWith(false)
            } else {
                // TODO: add tracking event
                canLoadMoreProducts = event.canLoadMore
                loadContinuation.continueWith(true)
            }
        }
    }
}
