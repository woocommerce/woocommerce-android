package com.woocommerce.android.ui.woopos.eligibility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

/**
 * Resolves a country code to the name WooCommerce uses for it, for display in the ineligible copy.
 *
 * Names come from the store's own country list, so they are already localised. Returns null when the
 * list has not been synced yet, which lets callers fall back to copy that names no country.
 */
class WooPosGetStoreCountryDisplayName @Inject constructor(
    private val dataStore: WCDataStore,
) {
    suspend operator fun invoke(countryCode: String): String? = withContext(Dispatchers.IO) {
        dataStore.getCountries()
            .firstOrNull { it.code.equals(countryCode, ignoreCase = true) }
            ?.name
            ?.takeIf { it.isNotBlank() }
    }
}
