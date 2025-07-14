package com.woocommerce.android.util

import javax.inject.Inject

class EmojiUtils @Inject constructor() {
    companion object {
        /**
         * The regional indicators go from 0x1F1E6 (A) to 0x1F1FF (Z).
         * This is the A regional indicator value minus 65 decimal so
         * that we can just add this to the A-Z char
         **/
        const val REGIONAL_INDICATOR_OFFSET = 0x1F1A5
    }

    /**
     * Given a two letter country code (ISO-3166) return a string emoji for the corresponding flag
     */
    fun countryCodeToEmojiFlag(countryCode: String): String {
        if (countryCode.length != 2) return countryCode
        val countryCodeCaps = countryCode.uppercase() // upper case is important because we are calculating offset
        val firstLetter = REGIONAL_INDICATOR_OFFSET + Character.codePointAt(countryCodeCaps, 0)
        val secondLetter = REGIONAL_INDICATOR_OFFSET + Character.codePointAt(countryCodeCaps, 1)
        return if (!countryCodeCaps[0].isLetter() || !countryCodeCaps[1].isLetter()) {
            countryCode
        } else {
            String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
        }
    }
}
