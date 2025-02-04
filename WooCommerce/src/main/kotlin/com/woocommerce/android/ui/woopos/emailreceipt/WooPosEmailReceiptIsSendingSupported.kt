package com.woocommerce.android.ui.woopos.emailreceipt

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class WooPosEmailReceiptIsSendingSupported @Inject constructor(
    private val getWooCoreVersion: GetWooCorePluginCachedVersion,
) {
    suspend operator fun invoke() = isWooCoreSupportsSendingReceiptsByEmail()

    private suspend fun isWooCoreSupportsSendingReceiptsByEmail() = withContext(Dispatchers.IO) {
        val wooCoreVersion = getWooCoreVersion()
        if (wooCoreVersion == null) {
            false
        } else {
            wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_SENDING_RECEIPTS_BY_EMAIL) >= 0
        }
    }

    companion object {
        const val WC_VERSION_SUPPORTS_SENDING_RECEIPTS_BY_EMAIL = "9.5.0"
    }
}
