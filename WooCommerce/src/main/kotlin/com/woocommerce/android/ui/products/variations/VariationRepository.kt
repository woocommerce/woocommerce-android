package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ERROR_DESC
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ID
import com.woocommerce.android.analytics.AnalyticsTracker.Stat
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_CREATION_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_CREATION_SUCCESS
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.model.toDataModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_PRODUCT_VARIATIONS
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnProductChanged
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class VariationRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val wooCommerceStore: WooCommerceStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val PRODUCT_VARIATIONS_PAGE_SIZE = WCProductStore.DEFAULT_PRODUCT_VARIATIONS_PAGE_SIZE
    }

    private var loadContinuation = ContinuationWrapper<Boolean>(WooLog.T.PRODUCTS)
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
    suspend fun fetchProductVariations(remoteProductId: Long, loadMore: Boolean = false): List<ProductVariation> {
        loadContinuation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            offset = if (loadMore) offset + PRODUCT_VARIATIONS_PAGE_SIZE else 0
            val payload = WCProductStore.FetchProductVariationsPayload(
                selectedSite.get(),
                remoteProductId,
                pageSize = PRODUCT_VARIATIONS_PAGE_SIZE,
                offset = offset
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchProductVariationsAction(payload))
        }

        return getProductVariationList(remoteProductId)
    }

    /**
     * Returns all product variations for a product and current site that are in the database
     */
    fun getProductVariationList(remoteProductId: Long): List<ProductVariation> {
        return productStore.getVariationsForProduct(selectedSite.get(), remoteProductId)
            .map { it.toAppModel() }
            .sorted()
    }

    /**
     * Returns the currency code for the site
     */
    fun getCurrencyCode() = wooCommerceStore.getSiteSettings(selectedSite.get())?.currencyCode

    /**
     * Fires the request to create a empty variation to a given product
     */
    suspend fun createEmptyVariation(product: Product): ProductVariation? =
        withContext(Dispatchers.IO) {
            productStore
                .generateEmptyVariation(selectedSite.get(), product.toDataModel())
                .handleVariationCreateResult(product)
        }

    private fun WooResult<WCProductVariationModel>.handleVariationCreateResult(
        product: Product
    ) = if (isError) {
        AnalyticsTracker.track(
            PRODUCT_VARIATION_CREATION_FAILED,
            mapOf(
                KEY_PRODUCT_ID to product.remoteId,
                KEY_ERROR_DESC to error.message
            )
        )
        null
    } else {
        AnalyticsTracker.track(
            PRODUCT_VARIATION_CREATION_SUCCESS,
            mapOf(KEY_PRODUCT_ID to product.remoteId)
        )
        model?.toAppModel()
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onProductChanged(event: OnProductChanged) {
        if (event.causeOfChange == FETCH_PRODUCT_VARIATIONS) {
            if (event.isError) {
                loadContinuation.continueWith(false)
                AnalyticsTracker.track(
                    Stat.PRODUCT_VARIANTS_LOAD_ERROR,
                    this.javaClass.simpleName,
                    event.error.type.toString(),
                    event.error.message
                )
            } else {
                canLoadMoreProductVariations = event.canLoadMore
                AnalyticsTracker.track(Stat.PRODUCT_VARIANTS_LOADED)
                loadContinuation.continueWith(true)
            }
        }
    }
}
