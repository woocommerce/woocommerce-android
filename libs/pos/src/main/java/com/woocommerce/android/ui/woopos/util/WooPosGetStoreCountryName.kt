package com.woocommerce.android.ui.woopos.util

import com.woocommerce.android.pos.R
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WooCommerceStore
import javax.inject.Inject

class WooPosGetStoreCountryName @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooCommerceStore: WooCommerceStore,
    private val resourceProvider: ResourceProvider,
) {
    suspend operator fun invoke(): String? = withContext(Dispatchers.IO) {
        val site = selectedSite.getOrNull() ?: return@withContext null
        val siteSettings = wooCommerceStore.getSiteSettings(site)
            ?: wooCommerceStore.fetchSiteGeneralSettings(site).model
        siteSettings?.countryCode?.let { countryCode ->
            getCountryNameFromCode(countryCode)
        }
    }

    private fun getCountryNameFromCode(countryCode: String): String? {
        return when (countryCode.lowercase()) {
            "us" -> resourceProvider.getString(R.string.woopos_eligibility_screen_US_country_name)
            "gb" -> resourceProvider.getString(R.string.woopos_eligibility_screen_UK_country_name)
            else -> null
        }
    }
}
