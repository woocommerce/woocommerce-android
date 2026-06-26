package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import com.woocommerce.android.util.WooLog
import org.wordpress.android.fluxc.model.refunds.RefundV4LineItem
import org.wordpress.android.fluxc.model.refunds.WCRefundPreview
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

class WooPosRefundPreview @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite,
    private val availabilityCache: WooPosV4RefundAvailabilityCache,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    suspend operator fun invoke(
        orderId: Long,
        lineItems: List<RefundV4LineItem>,
    ): Result {
        val site = selectedSite.get()
        val localSiteId = site.localId().value

        if (availabilityCache.isV4Available(localSiteId) == false) {
            return Result.FallbackToLocal
        }

        if (isWooVersionBelowV4Support()) {
            WooLog.i(WooLog.T.POS, "WooPosRefund: WooCommerce older than $MIN_WC_VERSION_FOR_V4; using v3")
            availabilityCache.markV4Unavailable(localSiteId)
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
                    WooLog.i(WooLog.T.POS, "WooPosRefund: v4 preview not available; falling back to v3")
                    availabilityCache.markV4Unavailable(localSiteId)
                    Result.FallbackToLocal
                } else {
                    WooLog.e(
                        WooLog.T.POS,
                        "WooPosRefund: v4 preview failed orderId=$orderId, type=${response.error.type}, " +
                            "message=${response.error.message}"
                    )
                    Result.Error
                }
            }

            preview != null -> {
                availabilityCache.markV4Available(localSiteId)
                Result.ServerCalculated(preview)
            }

            else -> Result.Error
        }
    }

    private fun isWooVersionBelowV4Support(): Boolean {
        val version = getWooCoreVersion() ?: return false
        return version.semverCompareTo(MIN_WC_VERSION_FOR_V4) < 0
    }

    sealed interface Result {
        data class ServerCalculated(val preview: WCRefundPreview) : Result
        data object FallbackToLocal : Result
        data object Error : Result
    }

    private companion object {
        private const val MIN_WC_VERSION_FOR_V4 = "10.9.0"
    }
}
