package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosSettingsStoreRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore
) {
    suspend fun getStoreInfo(): WooPosSettingsStoreState.StoreInfo =
        withContext(Dispatchers.IO) {
            val site = selectedSite.get()
            val settings = wooCommerceStore.getSiteSettings(site)
            val storeAddress = buildStoreAddress(settings)

            WooPosSettingsStoreState.StoreInfo(
                storeName = site.name ?: "",
                address = storeAddress
            )
        }

    @Suppress("CyclomaticComplexMethod")
    private fun buildStoreAddress(settings: Settings?): String {
        return buildString {
            settings?.address?.let { if (it.isNotBlank()) append(it) }
            settings?.address2?.let {
                if (it.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
            settings?.city?.let {
                if (it.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
            settings?.stateCode?.let {
                if (it.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
            settings?.postalCode?.let {
                if (it.isNotBlank()) {
                    if (isNotEmpty()) append(" ")
                    append(it)
                }
            }
            settings?.countryCode?.let {
                if (it.isNotBlank()) {
                    if (isNotEmpty()) append(", ")
                    append(it)
                }
            }
        }.ifBlank { "" }
    }
}
