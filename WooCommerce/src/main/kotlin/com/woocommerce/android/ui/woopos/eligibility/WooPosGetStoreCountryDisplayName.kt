package com.woocommerce.android.ui.woopos.eligibility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCDataStore
import java.util.Locale
import javax.inject.Inject

/**
 * Resolves a country code to a display name, for the ineligible copy that names the store's country.
 *
 * Prefers the store's own country list, so the name matches what WooCommerce shows. That list is
 * only synced by the address and shipping label flows, so a store that never opened them has none,
 * and the platform name for the code is used instead. iOS always has a name here, so falling back
 * keeps the copy the same on both.
 *
 * Returns null only when neither source knows the code, which lets callers use copy that names no
 * country.
 */
class WooPosGetStoreCountryDisplayName @Inject constructor(
    private val dataStore: WCDataStore,
) {
    suspend operator fun invoke(countryCode: String): String? = withContext(Dispatchers.IO) {
        storeCountryName(countryCode) ?: platformCountryName(countryCode)
    }

    private suspend fun storeCountryName(countryCode: String): String? =
        dataStore.getCountries()
            .firstOrNull { it.code.equals(countryCode, ignoreCase = true) }
            ?.name
            ?.takeIf { it.isNotBlank() }

    /**
     * Only for codes the platform actually knows. Locale.Builder rejects anything that is not shaped
     * like a region code, and an unassigned one that gets through resolves to "Unknown Region",
     * which would read as a country name in the copy.
     */
    private fun platformCountryName(countryCode: String): String? {
        val code = countryCode.uppercase(Locale.ROOT)
        if (code !in ISO_COUNTRIES) return null

        return Locale.Builder()
            .setRegion(code)
            .build()
            .displayCountry
            .takeIf { it.isNotBlank() && !it.equals(code, ignoreCase = true) }
    }

    private companion object {
        val ISO_COUNTRIES: Set<String> = Locale.getISOCountries().toSet()
    }
}
