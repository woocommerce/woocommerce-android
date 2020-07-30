package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.model.Variation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellationException
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

class VariationListRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
        private const val PRODUCT_VARIATIONS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
    }

    private var loadContinuation: Continuation<Boolean>? = null
    private var offset = 0

    var canLoadMoreProductVariations = true
        private set

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    /**
     * Submits a fetch request to get a list of products variations for the current site and productId
     * and returns the full list of product variations from the database
     */
    suspend fun fetchProductVariations(remoteProductId: Long, loadMore: Boolean = false): List<Variation> {
        try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                offset = if (loadMore) offset + PRODUCT_VARIATIONS_PAGE_SIZE else 0
                loadContinuation = it
                val payload = WCProductStore.FetchProductVariationsPayload(
                        selectedSite.get(),
                        remoteProductId,
                        pageSize = PRODUCT_VARIATIONS_PAGE_SIZE,
                        offset = offset
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(WooLog.T.PRODUCTS, "CancellationException while fetching product variations", e)
        }

        return getProductVariationList(remoteProductId)
    }

    /**
     * Returns all product variations for a product and current site that are in the database
     */
    fun getProductVariationList(remoteProductId: Long): List<Variation> {
        return productStore.getVariationsForProduct(selectedSite.get(), remoteProductId)
                .map { it.toAppModel() }
    }

    /**
     * Returns the currency code for the site
     */
    fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_VARIATIONS) {
            if (event.isError) {
                loadContinuation?.resume(false)
                AnalyticsTracker.track(
                        Stat.PRODUCT_VARIANTS_LOAD_ERROR,
                        this.javaClass.simpleName,
                        event.error.type.toString(),
                        event.error.message
                )
            } else {
                canLoadMoreProductVariations = event.canLoadMore
                AnalyticsTracker.track(Stat.PRODUCT_VARIANTS_LOADED)
                loadContinuation?.resume(true)
            }
            loadContinuation = null
        }
    }
}
