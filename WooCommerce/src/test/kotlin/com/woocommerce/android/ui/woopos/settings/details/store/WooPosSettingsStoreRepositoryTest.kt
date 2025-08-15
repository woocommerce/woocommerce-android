package com.woocommerce.android.ui.woopos.settings.details.store

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore

class WooPosSettingsStoreRepositoryTest {
    private val siteModel: SiteModel = mock()
    private val selectedSite: SelectedSite = mock {
        on { get() }.thenReturn(siteModel)
    }
    private val wooCommerceStore: WooCommerceStore = mock()

    private val repository = WooPosSettingsStoreRepository(
        selectedSite,
        wooCommerceStore
    )

    @Test
    fun `given complete site and settings data, when getStoreInfo, then returns full store info`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("My WooCommerce Store")
        whenever(siteModel.email).thenReturn("store@example.com")

        val settings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "US",
            stateCode = "NY",
            address = "123 Main Street",
            address2 = "Suite 456",
            city = "New York",
            postalCode = "10001",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.storeName).isEqualTo("My WooCommerce Store")
        assertThat(result.email).isEqualTo("store@example.com")
        assertThat(result.address).isEqualTo("123 Main Street, Suite 456, New York, NY 10001, US")
    }

    @Test
    fun `given site with null name and email, when getStoreInfo, then returns empty strings`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn(null)
        whenever(siteModel.email).thenReturn(null)
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.storeName).isEqualTo("")
        assertThat(result.email).isEqualTo("")
        assertThat(result.address).isEqualTo("")
    }

    @Test
    fun `given partial address data, when getStoreInfo, then returns formatted address with available fields`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("Test Store")
        whenever(siteModel.email).thenReturn("test@store.com")

        val settings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "US",
            stateCode = "CA",
            address = "456 Oak Avenue",
            address2 = "",
            city = "San Francisco",
            postalCode = "",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.storeName).isEqualTo("Test Store")
        assertThat(result.email).isEqualTo("test@store.com")
        assertThat(result.address).isEqualTo("456 Oak Avenue, San Francisco, CA, US")
    }

    @Test
    fun `given address with only street address, when getStoreInfo, then returns just street address`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("Minimal Store")
        whenever(siteModel.email).thenReturn("minimal@store.com")

        val settings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "",
            stateCode = "",
            address = "789 Pine Street",
            address2 = "",
            city = "",
            postalCode = "",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.address).isEqualTo("789 Pine Street")
    }

    @Test
    fun `given settings with all blank address fields, when getStoreInfo, then returns empty address`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("Empty Address Store")
        whenever(siteModel.email).thenReturn("empty@store.com")

        val settings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "",
            stateCode = "",
            address = "",
            address2 = "",
            city = "",
            postalCode = "",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.address).isEqualTo("")
    }

    @Test
    fun `given null settings, when getStoreInfo, then returns empty address`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("No Settings Store")
        whenever(siteModel.email).thenReturn("nosettings@store.com")
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(null)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.storeName).isEqualTo("No Settings Store")
        assertThat(result.email).isEqualTo("nosettings@store.com")
        assertThat(result.address).isEqualTo("")
    }

    @Test
    fun `given address with city and country only, when getStoreInfo, then returns city and country`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("International Store")
        whenever(siteModel.email).thenReturn("international@store.com")

        val settings = Settings(
            currencyCode = "GBP",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "UK",
            stateCode = "",
            address = "",
            address2 = "",
            city = "London",
            postalCode = "",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.address).isEqualTo(", London, UK")
    }

    @Test
    fun `given address with postal code and space formatting, when getStoreInfo, then formats postal code correctly`() = runTest {
        // GIVEN
        whenever(siteModel.name).thenReturn("Postal Store")
        whenever(siteModel.email).thenReturn("postal@store.com")

        val settings = Settings(
            currencyCode = "USD",
            currencyPosition = CurrencyPosition.LEFT,
            currencyThousandSeparator = ",",
            currencyDecimalSeparator = ".",
            currencyDecimalNumber = 2,
            countryCode = "US",
            stateCode = "PS",
            address = "100 Postal Road",
            address2 = "",
            city = "Postville",
            postalCode = "12345",
            couponsEnabled = true
        )
        whenever(wooCommerceStore.getSiteSettings(siteModel)).thenReturn(settings)

        // WHEN
        val result = repository.getStoreInfo()

        // THEN
        assertThat(result.address).isEqualTo("100 Postal Road, Postville, PS 12345, US")
    }
}
