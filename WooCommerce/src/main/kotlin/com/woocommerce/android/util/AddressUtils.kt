package com.woocommerce.android.util

import java.util.IllformedLocaleException
import java.util.Locale

object AddressUtils {
    /**
     * Translates a two-character country code into a human
     * readable label. If a full country name is provided instead of
     * a code, returns a best-effort display label without crashing.
     *
     * Examples:
     * - "US" -> "United States"
     * - "India " -> "India"
     */
    fun getCountryLabelByCountryCode(countryCode: String): String {
        if (countryCode.isBlank()) return ""

        val trimmed = countryCode.trim()

        // If it looks like a 2-letter country code, try to resolve safely
        if (trimmed.length == 2 && trimmed.all { it.isLetter() }) {
            val region = trimmed.uppercase(Locale.ROOT)
            return try {
                val locale = Locale.Builder()
                    .setLanguage(Locale.getDefault().language)
                    .setRegion(region)
                    .build()
                locale.displayCountry.ifBlank { region }
            } catch (_: IllformedLocaleException) {
                // Fall back to the original input if region is ill-formed
                trimmed
            }
        }

        // As a last resort, return the input as-is to avoid crashes
        return trimmed
    }
}
