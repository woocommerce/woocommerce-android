package com.woocommerce.android.ui.products

import com.woocommerce.android.annotations.OpenClassOnDebug
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

@OpenClassOnDebug
class ProductShippingRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val SHIPPING_CLASS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_SHIPPING_CLASS_PAGE_SIZE
    }

    private var shippingClassOffset = 0
    private var continuation: CancellableContinuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchProductShippingClasses(loadMore: Boolean = false) {
        try {
            continuation?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuation = it
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

        continuation = null
    }

    fun getCachedProductShippingClasses() =
            productStore.getShippingClassListForSite(selectedSite.get())

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductShiopingClassesChanged(event: OnProductShippingClassesChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_SHIPPING_CLASS_LIST) {
            if (event.isError) {
                continuation?.resume(false)
            } else {
                continuation?.resume(true)
            }
        }
    }
}
