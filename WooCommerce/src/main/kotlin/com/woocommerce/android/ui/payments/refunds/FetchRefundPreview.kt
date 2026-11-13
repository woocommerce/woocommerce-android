package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

/**
 * Asks the store for the totals of a refund before it is created. The preview request is also the probe for
 * server-calculated refunds: a success marks the store as supporting them, which unlocks the `compute_totals` create,
 * and a 404 (the route is missing before WooCommerce 11.1) marks it as not supporting them.
 */
class FetchRefundPreview @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val availabilityCache: ServerRefundAvailabilityCache,
    private val resolveRefundFlow: ResolveRefundFlow,
) {
    suspend operator fun invoke(orderId: Long, refundItems: List<RefundItem>): Result {
        val flow = resolveRefundFlow()
        val lineItems = refundItems.toRefundPreviewLineItems()
        if (flow !is RefundCalculationFlow.ServerComputed || lineItems.isEmpty()) {
            return Result.FallbackToLocal
        }

        val site = selectedSite.get()
        val response = refundStore.previewRefund(site, orderId, lineItems)
        val preview = response.model
        return when {
            response.isError && response.error.type == WooErrorType.API_NOT_FOUND -> {
                WooLog.i(WooLog.T.ORDERS, "Refund: preview route not available, falling back to local calculation")
                availabilityCache.markUnavailable(site.localId().value, flow.wooVersion)
                Result.FallbackToLocal
            }

            // Earlier refunds that weren't tied to items can leave less to refund than the selected lines are worth.
            // The local calculation caps the refund at what is left, while a computed create would be rejected.
            response.isError && response.error.apiErrorCode == PREVIEW_EXCEEDS_MAX_REFUNDABLE -> {
                WooLog.i(WooLog.T.ORDERS, "Refund: preview exceeds the remaining amount, using local calculation")
                Result.FallbackToLocal
            }

            response.isError -> {
                WooLog.e(
                    WooLog.T.ORDERS,
                    "Refund: preview failed orderId=$orderId, type=${response.error.type}, " +
                        "apiErrorCode=${response.error.apiErrorCode}, message=${response.error.message}"
                )
                Result.Error(storeMessage = response.error.message.storeMessageFor(response.error.apiErrorCode))
            }

            preview != null -> {
                availabilityCache.markAvailable(site.localId().value, flow.wooVersion)
                Result.ServerCalculated(preview)
            }

            else -> Result.Error(storeMessage = null)
        }
    }

    sealed interface Result {
        data class ServerCalculated(val preview: WCRefundPreview) : Result
        data object FallbackToLocal : Result

        /** [storeMessage] is the store's own explanation, when it rejected the refund for a refund-specific reason. */
        data class Error(val storeMessage: String?) : Result
    }

    companion object {
        private const val PREVIEW_EXCEEDS_MAX_REFUNDABLE = "woocommerce_rest_preview_exceeds_max_refundable"
    }
}

private const val STORE_REFUND_ERROR_PREFIX = "woocommerce_rest_"

/** The store's message for a WooCommerce refund error, or null for errors that are not the store's own. */
fun String?.storeMessageFor(apiErrorCode: String?): String? =
    takeIf { !it.isNullOrBlank() && apiErrorCode?.startsWith(STORE_REFUND_ERROR_PREFIX) == true }
