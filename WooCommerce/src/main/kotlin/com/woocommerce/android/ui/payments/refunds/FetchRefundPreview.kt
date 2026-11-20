package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

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

        data class Error(val storeMessage: String?) : Result
    }
}

private const val STORE_REFUND_ERROR_PREFIX = "woocommerce_rest_"

fun String?.storeMessageFor(apiErrorCode: String?): String? =
    takeIf { !it.isNullOrBlank() && apiErrorCode?.startsWith(STORE_REFUND_ERROR_PREFIX) == true }
