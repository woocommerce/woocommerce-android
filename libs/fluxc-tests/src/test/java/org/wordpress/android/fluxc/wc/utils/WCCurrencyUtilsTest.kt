package org.wordpress.android.fluxc.wc.utils

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.LEFT
import org.wordpress.android.fluxc.model.settings.CurrencyPosition.RIGHT_SPACE
import org.wordpress.android.fluxc.utils.WCCurrencyUtils.formatCurrencyForDisplay
import org.wordpress.android.fluxc.wc.settings.WCSettingsTestUtils.generateSettingsModel
import kotlin.test.assertEquals

class WCCurrencyUtilsTest {
    @Test
    @Suppress("LongMethod")
    fun testDecimalFormat() {
        val siteModel = SiteModel().apply { id = 6 }
        val cadSettings = generateSettingsModel(siteModel.localId()).copy(
            currencyCode = "CAD",
            currencyPosition = LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            couponsEnabled = true
        )

        val eurSettings = generateSettingsModel(siteModel.localId()).copy(
            currencyCode = "EUR",
            currencyPosition = RIGHT_SPACE,
            currencyThousandSeparator = ".",
            currencyDecimalSeparator = ",",
            currencyDecimalNumber = 2,
        )

        val jpySettings = generateSettingsModel(siteModel.localId()).copy(
            currencyCode = "JPY",
            currencyPosition = LEFT,
            currencyThousandSeparator = "",
            currencyDecimalSeparator = "",
            currencyDecimalNumber = 0,
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
