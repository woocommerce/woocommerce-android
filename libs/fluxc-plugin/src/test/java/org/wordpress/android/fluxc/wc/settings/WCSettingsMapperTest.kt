package org.wordpress.android.fluxc.wc.settings

import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.WCSettingsMapper
import org.wordpress.android.fluxc.persistence.entity.WCSettingsModel
import kotlin.test.assertEquals

internal class WCSettingsMapperTest {
    private val site = SiteModel().apply {
        id = 1
        siteId = 1337L
    }

    val mapper = WCSettingsMapper()

    @Test
    fun `mapper maps to correct settings model`() {
        // given
        val expectedModel = WCSettingsModel(
            localSiteId = site.localId(),
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 4,
            countryCode = "US",
            stateCode = "CA",
            address = "60 29th Street #343",
            address2 = "",
            city = "San Francisco",
            postalCode = "94110",
            couponsEnabled = true
        )

        // when
        val siteSettingsResponse = WCSettingsTestUtils.getSiteSettingsResponse()
        val result = mapper.mapSiteSettings(siteSettingsResponse!!, site)

        // then
        with(result) {
            assertEquals(expectedModel.localSiteId, localSiteId)
            assertEquals(expectedModel.currencyCode, currencyCode)
            assertEquals(expectedModel.currencyPosition, currencyPosition)
            assertEquals(expectedModel.currencyThousandSeparator, currencyThousandSeparator)
            assertEquals(expectedModel.currencyDecimalSeparator, currencyDecimalSeparator)
            assertEquals(expectedModel.currencyDecimalNumber, currencyDecimalNumber)
            assertEquals(expectedModel.countryCode, countryCode)
            assertEquals(expectedModel.stateCode, stateCode)
            assertEquals(expectedModel.address, address)
            assertEquals(expectedModel.address2, address2)
            assertEquals(expectedModel.city, city)
            assertEquals(expectedModel.postalCode, postalCode)
        }
    }

    @Test
    fun `mapper maps to correct product settings model`() {
        // given
        val expectedModel = WCProductSettingsModel(
            localSiteId = site.localId(),
            dimensionUnit = "in",
            weightUnit = "oz",
            defaultLowStockThreshold = 2
        )

        // when
        val siteProductSettingsResponse = WCSettingsTestUtils.getSiteProductSettingsResponse()
        val result = mapper.mapProductSettings(siteProductSettingsResponse!!, site)

        // then
        with(result) {
            assertEquals(expectedModel.localSiteId, localSiteId)
            assertEquals(expectedModel.dimensionUnit, dimensionUnit)
            assertEquals(expectedModel.weightUnit, weightUnit)
            assertEquals(expectedModel.defaultLowStockThreshold, defaultLowStockThreshold)
        }
    }

    @Test
    fun `when currency position is incorrect, then it maps to default`() {
        // given
        val siteProductResponse = WCSettingsTestUtils.getMalformedCurrencyPosSiteSettingsResponse()

        // when
        val result = mapper.mapSiteSettings(siteProductResponse!!, site)

        // then
        assertEquals(CurrencyPosition.LEFT, result.currencyPosition)
    }
}
