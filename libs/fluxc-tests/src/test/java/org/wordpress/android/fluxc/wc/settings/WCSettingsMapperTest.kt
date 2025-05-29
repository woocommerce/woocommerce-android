package org.wordpress.android.fluxc.wc.settings

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCProductSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel
import org.wordpress.android.fluxc.model.WCSettingsModel.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.WCSettingsMapper
import org.wordpress.android.fluxc.network.rest.wpcom.wc.SiteSettingOptionResponse
import kotlin.test.assertEquals

class WCSettingsMapperTest {
    private val site = SiteModel().apply {
        id = 1
        siteId = 1337L
    }

    val mapper = WCSettingsMapper()

    @Test
    fun `mapper maps to correct settings model`() {
        // given
        val expectedModel = WCSettingsModel(
            localSiteId = site.id,
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
        val expectedModel = WCProductSettingsModel().apply {
            localSiteId = site.id
            dimensionUnit = "in"
            weightUnit = "oz"
        }

        // when
        val siteProductSettingsResponse = WCSettingsTestUtils.getSiteProductSettingsResponse()
        val result = mapper.mapProductSettings(siteProductSettingsResponse!!, site)

        // then
        with(result) {
            assertEquals(expectedModel.localSiteId, localSiteId)
            assertEquals(expectedModel.dimensionUnit, dimensionUnit)
            assertEquals(expectedModel.weightUnit, weightUnit)
        }
    }

    @Test
    fun `when mapping feature is enabled settings with yes value, then returns true`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn("yes")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isTrue()
    }

    @Test
    fun `when mapping feature is enabled settings with no value, then returns false`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn("no")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isFalse()
    }

    @Test
    fun `when mapping feature is enabled settings with empty string, then returns null`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn("")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `when mapping feature is enabled settings with unknown value, then returns null`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn("maybe")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `when mapping feature is enabled settings with different casing, then returns null`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn("YES")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isNull()
    }

    @Test
    fun `when mapping feature is enabled settings with whitespace, then returns null`() {
        // Given
        val response = mock<SiteSettingOptionResponse>()
        whenever(response.value).thenReturn(" yes ")

        // When
        val result = mapper.mapFeatureIsEnabledSettings(response)

        // Then
        assertThat(result).isNull()
    }
}
