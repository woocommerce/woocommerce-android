package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.AppConstants
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.ContinuationWrapper
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Cancellation
import com.woocommerce.android.util.ContinuationWrapper.ContinuationResult.Success
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.WCProductAction.FETCH_SINGLE_VARIATION
import org.wordpress.android.fluxc.action.WCProductAction.UPDATED_VARIATION
import org.wordpress.android.fluxc.generated.WCProductActionBuilder
import org.wordpress.android.fluxc.model.WCProductVariationModel
import org.wordpress.android.fluxc.store.WCProductStore
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationChanged
import org.wordpress.android.fluxc.store.WCProductStore.OnVariationUpdated
import org.wordpress.android.fluxc.store.WCProductStore.ProductErrorType
import javax.inject.Inject

@OpenClassOnDebug
class VariationDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    private var continuationUpdateVariation = ContinuationWrapper<Boolean>(PRODUCTS)
    private var continuationFetchVariation = ContinuationWrapper<Boolean>(PRODUCTS)

    private var remoteProductId: Long = 0L
    private var remoteVariationId: Long = 0L

    var lastUpdateVariationErrorType: ProductErrorType? = null
    var lastFetchVariationErrorType: ProductErrorType? = null

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long): ProductVariation? {
        lastFetchVariationErrorType = null
        this.remoteProductId = remoteProductId
        this.remoteVariationId = remoteVariationId
        continuationFetchVariation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val payload = WCProductStore.FetchSingleVariationPayload(
                selectedSite.get(),
                remoteProductId,
                remoteVariationId
            )
            dispatcher.dispatch(WCProductActionBuilder.newFetchSingleVariationAction(payload))
        }
        return getVariation(remoteProductId, remoteVariationId)
    }

    /**
     * Fires the request to update the variation
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateVariation(updatedVariation: ProductVariation): Boolean {
        val result = continuationUpdateVariation.callAndWaitUntilTimeout(AppConstants.REQUEST_TIMEOUT) {
            val variation = updatedVariation.toDataModel(
                getCachedWCVariation(
                    updatedVariation.remoteProductId,
                    updatedVariation.remoteVariationId
                )
            )
            val payload = WCProductStore.UpdateVariationPayload(selectedSite.get(), variation)
            dispatcher.dispatch(WCProductActionBuilder.newUpdateVariationAction(payload))
        }
        return when (result) {
            is Cancellation -> false
            is Success -> result.value
        }
    }

    /**
     * Fires the request to delete a variation
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun deleteVariation(productID: Long, variationID: Long) =
        productStore
            .deleteVariation(selectedSite.get(), productID, variationID)
            .model?.let { true }
            ?: false

    private fun getCachedWCVariation(remoteProductId: Long, remoteVariationId: Long): WCProductVariationModel? =
        productStore.getVariationByRemoteId(selectedSite.get(), remoteProductId, remoteVariationId)

    fun getVariation(remoteProductId: Long, remoteVariationId: Long): ProductVariation? =
        getCachedWCVariation(remoteProductId, remoteVariationId)?.toAppModel()

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onVariationChanged(event: OnVariationChanged) {
        if (event.causeOfChange == FETCH_SINGLE_VARIATION &&
            event.remoteProductId == remoteProductId &&
            event.remoteVariationId == remoteVariationId) {
            if (continuationFetchVariation.isWaiting) {
                if (event.isError) {
                    lastFetchVariationErrorType = event.error?.type
                    continuationFetchVariation.continueWith(false)
                } else {
                    AnalyticsTracker.track(PRODUCT_VARIATION_LOADED)
                    continuationFetchVariation.continueWith(true)
                }
            } else {
                WooLog.w(PRODUCTS, "continuationFetchVariation is no longer active")
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onVariationUpdated(event: OnVariationUpdated) {
        if (event.causeOfChange == UPDATED_VARIATION) {
            if (event.isError) {
                AnalyticsTracker.track(
                    PRODUCT_VARIATION_UPDATE_ERROR, mapOf(
                    AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                    AnalyticsTracker.KEY_ERROR_TYPE to event.error?.type?.toString(),
                    AnalyticsTracker.KEY_ERROR_DESC to event.error?.message
                )
                )
                lastUpdateVariationErrorType = event.error?.type
                continuationUpdateVariation.continueWith(false)
            } else {
                AnalyticsTracker.track(PRODUCT_VARIATION_UPDATE_SUCCESS)
                continuationUpdateVariation.continueWith(true)
            }
        }
    }
}
