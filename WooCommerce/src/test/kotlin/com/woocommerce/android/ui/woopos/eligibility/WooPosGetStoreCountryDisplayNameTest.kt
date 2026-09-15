package com.woocommerce.android.ui.woopos.eligibility

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

class WooPosGetStoreCountryDisplayNameTest {

    private val dataStore: WCDataStore = mock()

    private val sut = WooPosGetStoreCountryDisplayName(dataStore)

    private fun country(code: String, name: String) = WCLocationModel(code = code, name = name)

    @Test
    fun `given the store country list is synced, when invoked, then the store's own name is used`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(listOf(country("CA", "Kanada")))

        assertThat(sut("CA")).isEqualTo("Kanada")
    }

    @Test
    fun `given a lowercase country code, when invoked, then it matches the store list case-insensitively`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(listOf(country("CA", "Kanada")))

        assertThat(sut("ca")).isEqualTo("Kanada")
    }

    @Test
    fun `given the country list has never been synced, when invoked, then the platform name is used`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(emptyList())

        assertThat(sut("CA")).isEqualTo("Canada")
    }

    @Test
    fun `given the country list has a blank name, when invoked, then the platform name is used`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(listOf(country("CA", "")))

        assertThat(sut("CA")).isEqualTo("Canada")
    }

    @Test
    fun `given a code no source knows, when invoked, then null is returned`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(emptyList())

        assertThat(sut("ZZ")).isNull()
    }

    @Test
    fun `given a code that is not a country code, when invoked, then null is returned`() = runTest {
        whenever(dataStore.getCountries()).thenReturn(emptyList())

        assertThat(sut("not a country")).isNull()
    }
}
