package com.woocommerce.android.ui.prefs.domain

import android.text.TextUtils

class DomainPhoneNumberUtils private constructor() {
    companion object {
        private const val PHONE_NUMBER_PREFIX = "+"
        private const val PHONE_NUMBER_CONNECTING_CHARACTER = "."

        private val countryCodeToPhoneNumberPrefixMap = mapOf(
            "AC" to 247,
            "AD" to 376,
            "AE" to 971,
            "AF" to 93,
            "AG" to 1268,
            "AI" to 1264,
            "AL" to 355,
            "AM" to 374,
            "AO" to 244,
            "AR" to 54,
            "AS" to 1684,
            "AT" to 43,
            "AU" to 61,
            "AW" to 297,
            "AX" to 358,
            "AZ" to 994,
            "BA" to 387,
            "BB" to 1246,
            "BD" to 880,
            "BE" to 32,
            "BF" to 226,
            "BG" to 359,
            "BH" to 973,
            "BI" to 257,
            "BJ" to 229,
            "BL" to 590,
            "BM" to 1441,
            "BN" to 673,
            "BO" to 591,
            "BQ" to 599,
            "BR" to 55,
            "BS" to 1242,
            "BT" to 975,
            "BV" to 47,
            "BW" to 267,
            "BY" to 375,
            "BZ" to 501,
            "CA" to 1,
            "CC" to 61,
            "CD" to 243,
            "CF" to 236,
            "CG" to 242,
            "CH" to 41,
            "CI" to 225,
            "CK" to 682,
            "CL" to 56,
            "CM" to 237,
            "CN" to 86,
            "CO" to 57,
            "CR" to 506,
            "CU" to 53,
            "CV" to 238,
            "CW" to 599,
            "CX" to 61,
            "CY" to 357,
            "CZ" to 420,
            "DE" to 49,
            "DJ" to 253,
            "DK" to 45,
            "DM" to 1767,
            "DZ" to 213,
            "EC" to 593,
            "EE" to 372,
            "EG" to 20,
            "ER" to 291,
            "ES" to 34,
            "ET" to 251,
            "FI" to 358,
            "FJ" to 679,
            "FK" to 500,
            "FM" to 691,
            "FO" to 298,
            "FR" to 33,
            "GA" to 241,
            "GB" to 44,
            "GD" to 1473,
            "GE" to 995,
            "GF" to 594,
            "GG" to 44,
            "GH" to 233,
            "GI" to 350,
            "GL" to 299,
            "GM" to 220,
            "GN" to 224,
            "GP" to 590,
            "GQ" to 240,
            "GR" to 30,
            "GS" to 500,
            "GT" to 502,
            "GU" to 1671,
            "GW" to 245,
            "GY" to 592,
            "HK" to 852,
            "HM" to 61,
            "HN" to 504,
            "HR" to 385,
            "HT" to 509,
            "HU" to 36,
            "ID" to 62,
            "IE" to 353,
            "IL" to 972,
            "IM" to 44,
            "IN" to 91,
            "IO" to 246,
            "IQ" to 964,
            "IR" to 98,
            "IS" to 354,
            "IT" to 39,
            "JE" to 44,
            "JM" to 1876,
            "JO" to 962,
            "JP" to 81,
            "KE" to 254,
            "KG" to 996,
            "KH" to 855,
            "KI" to 686,
            "KM" to 269,
            "KN" to 1869,
            "KP" to 850,
            "KR" to 82,
            "KV" to 383,
            "KW" to 965,
            "KY" to 1345,
            "KZ" to 7,
            "LA" to 856,
            "LB" to 961,
            "LC" to 1758,
            "LI" to 423,
            "LK" to 94,
            "LR" to 231,
            "LS" to 266,
            "LT" to 370,
            "LU" to 352,
            "LV" to 371,
            "LY" to 218,
            "MA" to 212,
            "MC" to 377,
            "MD" to 373,
            "ME" to 382,
            "MF" to 590,
            "MG" to 261,
            "MH" to 692,
            "MK" to 389,
            "ML" to 223,
            "MM" to 95,
            "MN" to 976,
            "MO" to 853,
            "MP" to 1670,
            "MQ" to 596,
            "MR" to 222,
            "MS" to 1664,
            "MT" to 356,
            "MU" to 230,
            "MV" to 960,
            "MW" to 265,
            "MX" to 52,
            "MY" to 60,
            "MZ" to 258,
            "NA" to 264,
            "NC" to 687,
            "NE" to 227,
            "NF" to 672,
            "NG" to 234,
            "NI" to 505,
            "NL" to 31,
            "NO" to 47,
            "NP" to 977,
            "NR" to 674,
            "NU" to 683,
            "NZ" to 64,
            "OM" to 968,
            "PA" to 507,
            "PE" to 51,
            "PF" to 689,
            "PG" to 675,
            "PH" to 63,
            "PK" to 92,
            "PL" to 48,
            "PM" to 508,
            "PN" to 64,
            "PS" to 970,
            "PT" to 351,
            "PW" to 680,
            "PY" to 595,
            "QA" to 974,
            "RO" to 40,
            "RS" to 381,
            "RU" to 7,
            "RW" to 250,
            "SA" to 966,
            "SB" to 677,
            "SC" to 248,
            "SD" to 249,
            "SE" to 46,
            "SG" to 65,
            "SH" to 290,
            "SI" to 386,
            "SJ" to 47,
            "SK" to 421,
            "SL" to 232,
            "SM" to 378,
            "SN" to 221,
            "SO" to 252,
            "SR" to 597,
            "SS" to 211,
            "ST" to 239,
            "SV" to 503,
            "SX" to 1721,
            "SY" to 963,
            "SZ" to 268,
            "TA" to 290,
            "TC" to 1649,
            "TD" to 235,
            "TF" to 262,
            "TG" to 228,
            "TH" to 66,
            "TJ" to 992,
            "TK" to 690,
            "TL" to 670,
            "TM" to 993,
            "TN" to 216,
            "TO" to 676,
            "TR" to 90,
            "TT" to 1868,
            "TV" to 688,
            "TW" to 886,
            "TZ" to 255,
            "UA" to 380,
            "UG" to 256,
            "UM" to 1,
            "US" to 1,
            "UY" to 598,
            "UZ" to 998,
            "VA" to 39,
            "VC" to 1784,
            "VE" to 58,
            "VG" to 1284,
            "VI" to 1340,
            "VN" to 84,
            "VU" to 678,
            "WF" to 681,
            "WS" to 685,
            "YE" to 967,
            "ZA" to 27,
            "ZM" to 260,
            "ZW" to 263
        )

        fun getPhoneNumberPrefix(countryCode: String): String? {
            if (countryCodeToPhoneNumberPrefixMap.containsKey(countryCode)) {
                return countryCodeToPhoneNumberPrefixMap[countryCode].toString()
            }
            return null
        }

        fun getPhoneNumberPrefixFromFullPhoneNumber(phoneNumber: String?): String? {
            val phoneParts = phoneNumber!!.split(PHONE_NUMBER_CONNECTING_CHARACTER)
            return if (TextUtils.isEmpty(phoneNumber)) {
                null
            } else if (phoneParts.size == 2) {
                var countryCode = phoneParts[0]
                if (countryCode.startsWith(PHONE_NUMBER_PREFIX)) {
                    countryCode = countryCode.drop(1)
                }

                countryCode
            } else {
                null
            }
        }

        fun getPhoneNumberWithoutPrefix(phoneNumber: String?): String? {
            val phoneParts = phoneNumber!!.split(PHONE_NUMBER_CONNECTING_CHARACTER)
            return if (TextUtils.isEmpty(phoneNumber)) {
                null
            } else if (phoneParts.size == 2) {
                val phoneNumberWithoutPrefix = phoneParts[1]
                if (!TextUtils.isEmpty(phoneNumberWithoutPrefix)) {
                    phoneNumberWithoutPrefix
                } else {
                    null
                }
            } else {
                null
            }
        }

        fun formatPhoneNumberandPrefix(phoneNumberPrefix: String?, phoneNumber: String?): String? {
            if (TextUtils.isEmpty(phoneNumberPrefix) && TextUtils.isEmpty(phoneNumber)) {
                return null
            }

            return PHONE_NUMBER_PREFIX + phoneNumberPrefix.orEmpty() + PHONE_NUMBER_CONNECTING_CHARACTER +
                phoneNumber.orEmpty()
        }
    }
}
