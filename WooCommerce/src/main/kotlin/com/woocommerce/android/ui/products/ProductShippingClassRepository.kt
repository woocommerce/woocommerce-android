package com.woocommerce.android.ui.products

import com.woocommerce.android.AppConstants
import com.woocommerce.android.model.ShippingClass
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_SHIPPING_CLASS_LIST
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductShippingClassListPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductShippingClassesChanged
import javax.inject.Inject
import kotlin.coroutines.resume

class ProductShippingClassRepository@Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val SHIPPING_CLASS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE
    }

    private var continuationShippingClasses: CancellableContinuation<Boolean>? = null

    private var shippingClassOffset = 0
    final var canLoadMoreShippingClasses = true
        private set

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Fetches the list of shipping classes for the [selectedSite], optionally loading the next page of classes
     */
    suspend fun fetchShippingClassesForSite(loadMore: Boolean = false): List<ShippingClass> {
        try {
            continuationShippingClasses?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(AppConstants.REQUEST_TIMEOUT) {
                continuationShippingClasses = it
                shippingClassOffset = if (loadMore) {
                    shippingClassOffset + SHIPPING_CLASS_PAGE_SIZE
                } else {
                    0
                }
                val payload = FetchProductShippingClassListPayload(
                        selectedSite.get(),
                        pageSize = SHIPPING_CLASS_PAGE_SIZE,
                        offset = shippingClassOffset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductShippingClassListAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.d(PRODUCTS, "CancellationException while fetching product shipping classes")
        }

        continuationShippingClasses = null
        return getProductShippingClassesForSite()
    }

    /**
     * Returns a list of cached (SQLite) shipping classes for the current site
     */
    fun getProductShippingClassesForSite(): List<ShippingClass> =
            productStore.getShippingClassListForSite(selectedSite.get()).map { it.toAppModel() }

    /**
     * The list of shipping classes has been fetched for the current site
     */
    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductShippingClassesChanged(event: OnProductShippingClassesChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_SHIPPING_CLASS_LIST) {
            canLoadMoreShippingClasses = event.canLoadMore
            if (event.isError) {
                continuationShippingClasses?.resume(false)
            } else {
                continuationShippingClasses?.resume(true)
            }
        }
    }
}
