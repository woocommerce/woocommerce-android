package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCDataStore
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GetAllCountriesTest : BaseUnitTest() {
    private val dataStore: WCDataStore = mock()
    private val selectedSite: SelectedSite = mock()

    private val sut = GetAllCountries(dataStore, selectedSite, coroutinesTestRule.testDispatchers)

    private val countries = listOf(
        WCLocationModel(1).also {
            it.code = "US"
            it.name = "United States"
        },
        WCLocationModel(2).also {
            it.code = "UK"
            it.name = "United Kingdom"
        },
        WCLocationModel(3).also {
            it.code = "AR"
            it.name = "Argentina"
        },
        WCLocationModel(4).also {
            it.code = "BR"
            it.name = "Brazil"
        }
    )

    @Test
    fun `when there is no cached data and selected site is null then return failure`() = testBlocking {
        whenever(dataStore.getCountries()).doReturn(emptyList())
        whenever(selectedSite.getOrNull()).doReturn(null)

        val result = sut.invoke()

        assert(result.isFailure)
    }

    @Test
    fun `when there is cached data then return cached data`() = testBlocking {
        whenever(dataStore.getCountries()).doReturn(countries)

        val result = sut.invoke()

        assert(result.isSuccess)
        val cachedCountries = result.getOrNull()
        assertThat(cachedCountries).isNotNull
        assertThat(cachedCountries!!.size).isEqualTo(countries.size)
        val cachedCountriesCode = cachedCountries.map { it.code }.toSet()
        val countriesCode = countries.map { it.code }.toSet()
        assertThat(cachedCountriesCode).isEqualTo(countriesCode)
        verify(dataStore, never()).fetchCountriesAndStates(any())
    }

    @Test
    fun `when there is NO cached data then fetch countries from server and return`() = testBlocking {
        whenever(dataStore.getCountries()).doReturn(emptyList())
        whenever(selectedSite.getOrNull()).doReturn(SiteModel())
        whenever(dataStore.fetchCountriesAndStates(any())).doReturn(WooResult(countries))

        val result = sut.invoke()

        assert(result.isSuccess)
        val apiCountries = result.getOrNull()
        assertThat(apiCountries).isNotNull
        assertThat(apiCountries!!.size).isEqualTo(countries.size)
        val apiCountriesCode = apiCountries.map { it.code }.toSet()
        val countriesCode = countries.map { it.code }.toSet()
        assertThat(apiCountriesCode).isEqualTo(countriesCode)
        verify(dataStore).fetchCountriesAndStates(any())
    }

    @Test
    fun `when there is NO cached data and fetch countries from server fails then return failure`() = testBlocking {
        whenever(dataStore.getCountries()).doReturn(emptyList())
        whenever(selectedSite.getOrNull()).doReturn(SiteModel())
        whenever(dataStore.fetchCountriesAndStates(any())).doReturn(
            WooResult(
                WooError(WooErrorType.GENERIC_ERROR, GenericErrorType.UNKNOWN, "error")
            )
        )

        val result = sut.invoke()

        assert(result.isFailure)
        verify(dataStore).fetchCountriesAndStates(any())
    }
}
