package com.woocommerce.android.util

import java.util.IllformedLocaleException
import java.util.Locale

object AddressUtils {
    /**
     * Returns a human-readable country label for the given input.
     *
     * Behavior:
     * - If input is blank -> returns empty string.
     * - If input looks like a 2-letter ISO country code (e.g., "US", "gb") ->
     *   returns the localized display name (e.g., "United States"). Falls back
     *   to the original input if it cannot be resolved.
     * - Otherwise (e.g., full country name like "India") -> returns the trimmed input.
     */
    fun getCountryLabelByCountryCode(countryCode: String): String {
        val value = countryCode.trim()
        if (value.isEmpty() || !isIsoLikeCountryCode(value)) return value

        val region = value.uppercase(Locale.ROOT)
        return resolveDisplayCountry(region, fallback = value)
    }

    private fun isIsoLikeCountryCode(input: String): Boolean =
        input.length == 2 && input.all { it.isLetter() }

    private fun resolveDisplayCountry(region: String, fallback: String): String =
        try {
            val locale = Locale.Builder()
                .setLanguage(Locale.getDefault().language)
                .setRegion(region)
                .build()
            val display = locale.displayCountry
            display.ifBlank { fallback }
        } catch (_: IllformedLocaleException) {
            // If the region is ill-formed, return the safest fallback (original input)
            fallback
        }
}
