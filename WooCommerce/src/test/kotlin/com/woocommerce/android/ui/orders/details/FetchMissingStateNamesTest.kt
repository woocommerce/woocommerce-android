package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.UNKNOWN
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.store.WCDataStore

@OptIn(ExperimentalCoroutinesApi::class)
class FetchMissingStateNamesTest : BaseUnitTest() {
    private val site = SiteModel()
    private val dataStore: WCDataStore = mock()
    private val selectedSite: SelectedSite = mock {
        on { getOrNull() } doReturn site
    }

    private val sut = FetchMissingStateNames(dataStore, selectedSite)

    @Test
    fun `given a Chinese address with an unresolved state and no stored China, when invoked, then fetches countries`() =
        testBlocking {
            whenever(dataStore.getCountry("CN")).thenReturn(null)
            whenever(dataStore.fetchCountriesAndStates(site)).thenReturn(WooResult(emptyList()))

            val result = sut(listOf(address(country = "CN", state = AmbiguousLocation.Raw("CN24"))))

            assertThat(result).isTrue()
            verify(dataStore).fetchCountriesAndStates(site)
        }

    @Test
    fun `given China is already stored, when invoked, then does not fetch countries`() = testBlocking {
        whenever(dataStore.getCountry("CN")).thenReturn(WCLocationModel(code = "CN", name = "China"))

        val result = sut(listOf(address(country = "CN", state = AmbiguousLocation.Raw("CN99"))))

        assertThat(result).isFalse()
        verify(dataStore, never()).fetchCountriesAndStates(any())
    }

    @Test
    fun `given a US address with an unresolved state, when invoked, then does not fetch countries`() = testBlocking {
        val result = sut(listOf(address(country = "US", state = AmbiguousLocation.Raw("CA"))))

        assertThat(result).isFalse()
        verify(dataStore, never()).fetchCountriesAndStates(any())
    }

    @Test
    fun `given a Chinese address with a resolved state, when invoked, then does not fetch countries`() = testBlocking {
        val state = AmbiguousLocation.Defined(Location(code = "CN24", name = "Sichuan / 四川", parentCode = "CN"))

        val result = sut(listOf(address(country = "CN", state = state)))

        assertThat(result).isFalse()
        verify(dataStore, never()).fetchCountriesAndStates(any())
    }

    @Test
    fun `given a Chinese address without a state, when invoked, then does not fetch countries`() = testBlocking {
        val result = sut(listOf(address(country = "CN", state = AmbiguousLocation.EMPTY)))

        assertThat(result).isFalse()
        verify(dataStore, never()).fetchCountriesAndStates(any())
    }

    @Test
    fun `given fetching countries fails, when invoked, then returns false`() = testBlocking {
        whenever(dataStore.getCountry("CN")).thenReturn(null)
        whenever(dataStore.fetchCountriesAndStates(site)).thenReturn(WooResult(WooError(GENERIC_ERROR, UNKNOWN)))

        val result = sut(listOf(address(country = "CN", state = AmbiguousLocation.Raw("CN24"))))

        assertThat(result).isFalse()
    }

    private fun address(country: String, state: AmbiguousLocation) =
        Address.EMPTY.copy(country = Location(code = country, name = country), state = state)
}
