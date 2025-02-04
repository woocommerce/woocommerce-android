package org.wordpress.android.fluxc.wc.utils

import org.junit.Test
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.utils.WCCurrencyUtils.formatCurrencyForDisplay
import kotlin.test.assertEquals

class WCCurrencyUtilsTest {
    @Test
    @Suppress("LongMethod")
    fun testDecimalFormat() {
        val cadSettings = WCSettingsModel(
                localSiteId = 6,
                currencyCode = "CAD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                couponsEnabled = true
        )

        val eurSettings = WCSettingsModel(
                localSiteId = 6,
                currencyCode = "EUR",
                currencyPosition = CurrencyPosition.RIGHT_SPACE,
                currencyThousandSeparator = ".",
                currencyDecimalSeparator = ",",
                currencyDecimalNumber = 2,
                couponsEnabled = true
        )

        val jpySettings = WCSettingsModel(
                localSiteId = 6,
                currencyCode = "JPY",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = "",
                currencyDecimalSeparator = "",
                currencyDecimalNumber = 0,
                couponsEnabled = true
        )

        with(3.22) {
            val cadFormat = formatCurrencyForDisplay(this, cadSettings)
            assertEquals("3.22", cadFormat)

            val eurFormat = formatCurrencyForDisplay(this, eurSettings)
            assertEquals("3,22", eurFormat)

            val jpyFormat = formatCurrencyForDisplay(this, jpySettings)
            assertEquals("3", jpyFormat)
        }

        with(1234.22) {
            val cadFormat = formatCurrencyForDisplay(this, cadSettings)
            assertEquals("1,234.22", cadFormat)

            val eurFormat = formatCurrencyForDisplay(this, eurSettings)
            assertEquals("1.234,22", eurFormat)

            val jpyFormat = formatCurrencyForDisplay(this, jpySettings)
            assertEquals("1234", jpyFormat)
        }

        with(1234.toDouble()) {
            val cadFormat = formatCurrencyForDisplay(this, cadSettings)
            assertEquals("1,234.00", cadFormat)

            val eurFormat = formatCurrencyForDisplay(this, eurSettings)
            assertEquals("1.234,00", eurFormat)

            val jpyFormat = formatCurrencyForDisplay(this, jpySettings)
            assertEquals("1234", jpyFormat)
        }

        with(1234567.11) {
            val cadFormat = formatCurrencyForDisplay(this, cadSettings)
            assertEquals("1,234,567.11", cadFormat)

            val eurFormat = formatCurrencyForDisplay(this, eurSettings)
            assertEquals("1.234.567,11", eurFormat)

            val jpyFormat = formatCurrencyForDisplay(this, jpySettings)
            assertEquals("1234567", jpyFormat)
        }

        with(-1234.22) {
            val cadFormat = formatCurrencyForDisplay(this, cadSettings)
            assertEquals("-1,234.22", cadFormat)

            val eurFormat = formatCurrencyForDisplay(this, eurSettings)
            assertEquals("-1.234,22", eurFormat)

            val jpyFormat = formatCurrencyForDisplay(this, jpySettings)
            assertEquals("-1234", jpyFormat)
        }
    }
}
