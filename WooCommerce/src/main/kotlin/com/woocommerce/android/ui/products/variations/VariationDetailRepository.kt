package com.woocommerce.android.ui.products.variations

import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_LOADED
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_ERROR
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.PRODUCT_VARIATION_UPDATE_SUCCESS
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.ProductVariation
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T.PRODUCTS
import com.woocommerce.android.util.suspendCancellableCoroutineWithTimeout
import com.woocommerce.android.util.suspendCoroutineWithTimeout
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
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
import javax.inject.Inject
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

@OpenClassOnDebug
class VariationDetailRepository @Inject constructor(
    private val dispatcher: Dispatcher,
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    companion object {
        private const val ACTION_TIMEOUT = 10L * 1000
    }

    private var continuationUpdateVariation: Continuation<Boolean>? = null
    private var continuationFetchVariation: CancellableContinuation<Boolean>? = null

    private var remoteProductId: Long = 0L
    private var remoteVariationId: Long = 0L

    init {
        dispatcher.register(this)
    }

    fun onCleanup() {
        dispatcher.unregister(this)
    }

    suspend fun fetchVariation(remoteProductId: Long, remoteVariationId: Long): ProductVariation? {
        try {
            this.remoteProductId = remoteProductId
            this.remoteVariationId = remoteVariationId
            continuationFetchVariation?.cancel()
            suspendCancellableCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationFetchVariation = it

                val payload = WCProductStore.FetchSingleVariationPayload(
                    selectedSite.get(),
                    remoteProductId,
                    remoteVariationId
                )
                dispatcher.dispatch(WCProductActionBuilder.newFetchSingleVariationAction(payload))
            }
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "CancellationException while fetching single variation")
        }

        continuationFetchVariation = null
        return getVariation(remoteProductId, remoteVariationId)
    }

    /**
     * Fires the request to update the variation
     *
     * @return the result of the action as a [Boolean]
     */
    suspend fun updateVariation(updatedVariation: ProductVariation): Boolean {
        return try {
            suspendCoroutineWithTimeout<Boolean>(ACTION_TIMEOUT) {
                continuationUpdateVariation = it

                val variation = updatedVariation.toDataModel(getCachedVariation(
                    updatedVariation.remoteProductId,
                    updatedVariation.remoteVariationId
                ))
                val payload = WCProductStore.UpdateVariationPayload(selectedSite.get(), variation)
                dispatcher.dispatch(WCProductActionBuilder.newUpdateVariationAction(payload))
            } ?: false // request timed out
        } catch (e: CancellationException) {
            WooLog.e(PRODUCTS, "Exception encountered while updating variation", e)
            false
        }
    }

    private fun getCachedVariation(remoteProductId: Long, remoteVariationId: Long): WCProductVariationModel? =
        productStore.getVariationByRemoteId(selectedSite.get(), remoteProductId, remoteVariationId)

    fun getVariation(remoteProductId: Long, remoteVariationId: Long): ProductVariation? =
        getCachedVariation(remoteProductId, remoteVariationId)?.toAppModel()

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onVariationChanged(event: OnVariationChanged) {
        if (event.causeOfChange == FETCH_SINGLE_VARIATION &&
            event.remoteProductId == remoteProductId &&
            event.remoteVariationId == remoteVariationId) {
            if (continuationFetchVariation?.isActive == true) {
                if (event.isError) {
                    continuationFetchVariation?.resume(false)
                } else {
                    AnalyticsTracker.track(PRODUCT_VARIATION_LOADED)
                    continuationFetchVariation?.resume(true)
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
                        AnalyticsTracker.KEY_ERROR_DESC to event.error?.message))
                continuationUpdateVariation?.resume(false)
            } else {
                AnalyticsTracker.track(PRODUCT_VARIATION_UPDATE_SUCCESS)
                continuationUpdateVariation?.resume(true)
            }
            continuationUpdateVariation = null
        }
    }
}
