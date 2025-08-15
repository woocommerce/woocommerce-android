package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosReceiptRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion
) {
    suspend fun getReceiptInfo(): WooPosReceiptDataResult =
        withContext(Dispatchers.IO) {
            if (!isWooCommerceVersionAtLeast10()) {
                return@withContext WooPosReceiptDataResult.NotAvailable
            }

            try {
                val site = selectedSite.get()
                val response = wooCommerceStore.fetchPosSettings(site)

                if (response.isError) {
                    return@withContext WooPosReceiptDataResult.Error
                }

                val settings = response.model ?: emptyMap()
                val receiptInfo = WooPosSettingsStoreState.ReceiptInfo(
                    storeName = settings["woocommerce_pos_store_name"] ?: "",
                    address = settings["woocommerce_pos_store_address"] ?: "",
                    phone = settings["woocommerce_pos_store_phone"] ?: "",
                    email = settings["woocommerce_pos_store_email"] ?: "",
                    refundPolicy = settings["woocommerce_pos_refund_returns_policy"] ?: ""
                )

                WooPosReceiptDataResult.Success(receiptInfo)
            } catch (_: Exception) {
                WooPosReceiptDataResult.Error
            }
        }

    private fun isWooCommerceVersionAtLeast10(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_RECEIPTS) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_RECEIPTS = "10.0.0"
    }
}

sealed class WooPosReceiptDataResult {
    data class Success(val receiptInfo: WooPosSettingsStoreState.ReceiptInfo) : WooPosReceiptDataResult()
    data object NotAvailable : WooPosReceiptDataResult()
    data object Error : WooPosReceiptDataResult()
}
