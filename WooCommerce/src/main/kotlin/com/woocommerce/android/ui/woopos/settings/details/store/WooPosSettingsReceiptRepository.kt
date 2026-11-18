package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosSettingsReceiptRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion
) {
    private val cachedReceiptInfo = ConcurrentHashMap<Int, WooPosSettingsStoreState.ReceiptInfo>()

    fun getCachedReceiptInfo(site: SiteModel): WooPosSettingsStoreState.ReceiptInfo? = cachedReceiptInfo[site.id]

    suspend fun getReceiptInfo(): WooPosReceiptDataResult = getReceiptInfo(selectedSite.get())

    suspend fun getReceiptInfo(site: SiteModel): WooPosReceiptDataResult =
        withContext(Dispatchers.IO) {
            if (!isWooCommerceVersionAtLeast10(site)) {
                return@withContext WooPosReceiptDataResult.NotAvailable
            }

            try {
                val response = wooCommerceStore.fetchPosSettings(site)

                if (response.isError) {
                    return@withContext WooPosReceiptDataResult.Error
                }

                val settings = response.model ?: emptyMap()
                val receiptInfo = WooPosSettingsStoreState.ReceiptInfo(
                    storeName = settings["woocommerce_pos_store_name"] ?: "",
                    address = formatReceiptAddress(settings["woocommerce_pos_store_address"] ?: ""),
                    phone = settings["woocommerce_pos_store_phone"] ?: "",
                    email = settings["woocommerce_pos_store_email"] ?: "",
                    refundPolicy = settings["woocommerce_pos_refund_returns_policy"] ?: ""
                )
                cachedReceiptInfo[site.id] = receiptInfo

                WooPosReceiptDataResult.Success(receiptInfo)
            } catch (_: Exception) {
                WooPosReceiptDataResult.Error
            }
        }

    private fun formatReceiptAddress(address: String): String {
        return address.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(", ")
            .replace(Regex(",\\s*,"), ",")
    }

    private fun isWooCommerceVersionAtLeast10(site: SiteModel): Boolean {
        val wooCoreVersion = getWooCoreVersion(site) ?: return false
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
