package com.woocommerce.android.ui.products

import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Product
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
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_SKU_AVAILABILITY
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.FetchProductSkuAvailabilityPayload
import org.wordpress.android.fluxc.store.WCProductStore.OnProductSkuAvailabilityChanged
import javax.inject.Inject
import kotlin.coroutines.resume

@OpenClassOnDebug
class ProductInventoryRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationVerifySku: CancellableContinuation<Boolean>? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Fires the request to check if sku is available for a given [selectedSite]
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun verifySkuAvailability(sku: String): Boolean? {
        continuationVerifySku?.cancel()
        return try {
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationVerifySku = it

                val payload = FetchProductSkuAvailabilityPayload(selectedSite.get(), sku)
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductSkuAvailabilityAction(payload))
            } // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while verifying product sku availability", e)
            null
        }
    }

    private fun getCachedWCProductModel(remoteProductId: Long) =
            productStore.getProductByRemoteId(selectedSite.get(), remoteProductId)

    fun getProduct(remoteProductId: Long): Product? = getCachedWCProductModel(remoteProductId)?.toAppModel()

    fun geProductExistsBySku(sku: String) = productStore.geProductExistsBySku(selectedSite.get(), sku)

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onProductSkuAvailabilityChanged(event: OnProductSkuAvailabilityChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_SKU_AVAILABILITY) {
            // TODO: add event to track sku availability success
            continuationVerifySku?.resume(event.available)
            continuationVerifySku = null
        }
    }
}
