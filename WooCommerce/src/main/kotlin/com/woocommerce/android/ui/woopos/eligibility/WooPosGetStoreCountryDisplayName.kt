package com.woocommerce.android.ui.woopos.eligibility

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCDataStore
import java.util.Locale
import javax.inject.Inject

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
