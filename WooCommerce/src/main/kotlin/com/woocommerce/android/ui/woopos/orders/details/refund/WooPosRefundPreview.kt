package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsEvent
import com.woocommerce.android.ui.woopos.util.analytics.WooPosAnalyticsTracker
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.RefundPreviewLineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

/**
 * Fetches a server-calculated refund preview, probing the store for server-refund support.
 *
 * The store's eligibility is decided by [WooPosResolveRefundFlow]; when eligible, the preview
 * request doubles as the availability probe: a success marks the store available (unlocking the
 * computed create), a 404 (`rest_no_route` on stores older than the release shipping the preview
 * route) marks it unavailable and falls back to the local-calculation flow.
 */
class WooPosRefundPreview @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val availabilityCache: WooPosServerRefundAvailabilityCache,
    private val resolveRefundFlow: WooPosResolveRefundFlow,
    private val analyticsTracker: WooPosAnalyticsTracker,
) {
    suspend operator fun invoke(
        orderId: Long,
        lineItems: List<RefundPreviewLineItem>,
    ): Result {
        val site = selectedSite.get()
        val localSiteId = site.localId().value

        val flow = resolveRefundFlow()
        if (flow !is WooPosRefundFlow.ServerComputed) {
            return Result.FallbackToLocal
        }

        if (lineItems.isEmpty()) {
            return Result.FallbackToLocal
        }

        val response = refundStore.previewRefund(site, orderId, lineItems)
        val preview = response.model
        return when {
            response.isError -> {
                if (response.error.type == WooErrorType.API_NOT_FOUND) {
                    WooLog.i(WooLog.T.POS, "WooPosRefund: preview route not available; falling back to local")
                    availabilityCache.markUnavailable(localSiteId, flow.wooVersion)
                    // Reported once per store per WooCommerce version for the lifetime of the
                    // process: the availability cache short-circuits the resolver afterwards, so
                    // this counts stores that fell back rather than the refunds they made after.
                    // The store's version arrives with the event as the cached_woo_core_version
                    // property every event carries.
                    analyticsTracker.track(WooPosAnalyticsEvent.Event.RefundServerFlowUnavailable)
                    Result.FallbackToLocal
                } else {
                    WooLog.e(
                        WooLog.T.POS,
                        "WooPosRefund: preview failed orderId=$orderId, type=${response.error.type}, " +
                            "apiErrorCode=${response.error.apiErrorCode}, " +
                            "message=${response.error.message}"
                    )
                    Result.Error(WooPosRefundApiError.fromCode(response.error.apiErrorCode))
                }
            }

            preview != null -> {
                availabilityCache.markAvailable(localSiteId, flow.wooVersion)
                Result.ServerCalculated(preview)
            }

            else -> Result.Error()
        }
    }

    sealed interface Result {
        data class ServerCalculated(val preview: WCRefundPreview) : Result
        data object FallbackToLocal : Result

        data class Error(val apiError: WooPosRefundApiError? = null) : Result
    }
}
