package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.extensions.semverCompareTo
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.GetWooCorePluginCachedVersion
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosStoreReceiptRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val getWooCoreVersion: GetWooCorePluginCachedVersion
) {
    suspend fun getStoreData(): WooPosStoreDataResult =
        withContext(Dispatchers.IO) {
            try {
                val site = selectedSite.get()

                val settings = wooCommerceStore.getSiteSettings(site)
                val storeAddress = buildStoreAddress(settings)

                val storeInfo = StoreInfo(
                    storeName = site.name ?: "",
                    address = storeAddress,
                    phone = "",
                    email = site.email ?: ""
                )

                val receiptInfo = if (isWooCommerceVersionAtLeast10()) {
                    ReceiptInfo(
                        storeName = site.name ?: "",
                        address = storeAddress,
                        phone = "",
                        email = site.email ?: "",
                        refundPolicy = ""
                    )
                } else {
                    null
                }

                WooPosStoreDataResult.Success(storeInfo, receiptInfo)
            } catch (_: Exception) {
                WooPosStoreDataResult.Error
            }
        }

    private fun buildStoreAddress(settings: Settings?): String {
        return buildString {
            settings?.address?.let { if (it.isNotBlank()) append(it) }
            settings?.address2?.let { if (it.isNotBlank()) append(", $it") }
            settings?.city?.let { if (it.isNotBlank()) append(", $it") }
            settings?.stateCode?.let { if (it.isNotBlank()) append(", $it") }
            settings?.postalCode?.let { if (it.isNotBlank()) append(" $it") }
            settings?.countryCode?.let { if (it.isNotBlank()) append(", $it") }
        }.ifBlank { "" }
    }

    private fun isWooCommerceVersionAtLeast10(): Boolean {
        val wooCoreVersion = getWooCoreVersion() ?: return false
        return wooCoreVersion.semverCompareTo(WC_VERSION_SUPPORTS_RECEIPTS) >= 0
    }

    private companion object {
        const val WC_VERSION_SUPPORTS_RECEIPTS = "10.0.0"
    }
}

sealed class WooPosStoreDataResult {
    data class Success(
        val storeInfo: StoreInfo,
        val receiptInfo: ReceiptInfo? = null
    ) : WooPosStoreDataResult()

    data object NotAvailable : WooPosStoreDataResult()
    data object Error : WooPosStoreDataResult()
}
