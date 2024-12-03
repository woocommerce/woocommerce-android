package com.woocommerce.android.ui.woopos.home.totals.payment.receipt

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.ui.woopos.featureflags.WooPosIsReceiptsEnabled
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosTotalsPaymentReceiptIsSendingAvailable @Inject constructor(
    private val isReceiptsEnabled: WooPosIsReceiptsEnabled,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    suspend operator fun invoke() =
        if (!isReceiptsEnabled()) {
            false
        } else {
            isWooCoreSupportsSendingReceiptsByEmail()
        }

    private suspend fun isWooCoreSupportsSendingReceiptsByEmail() = withContext(Dispatchers.IO) {
        val wooCoreVersion = getWooCoreVersion()
        if (wooCoreVersion == null) {
            false
        } else {
            wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_SENDING_RECEIPTS_BY_EMAIL) >= 0
        }
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_SENDING_RECEIPTS_BY_EMAIL = "9.5.0"
    }
}
