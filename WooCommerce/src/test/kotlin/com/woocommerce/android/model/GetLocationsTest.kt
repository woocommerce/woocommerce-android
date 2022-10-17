package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

class GetLocationsTest {
    private lateinit var sut: GetLocations
    private val locationStore: WCDataStore = mock()

    @Before
    fun setUp() {
        sut = GetLocations(locationStore)
    }

    @Test
    fun `should provide country and associated state if locations are in database`() {
        // given
        val country = WCLocationModel().apply {
            code = "US"
            name = "United States"
        }
        val associatedState = WCLocationModel().apply {
            parentCode = "US"
            code = "CA"
            name = "California"
        }
        whenever(locationStore.getCountries()).thenReturn(listOf(country))
        whenever(locationStore.getStates(country.code)).thenReturn(listOf(associatedState))

        // when
        val (resultCountry, resultState) = sut.invoke(country.code, associatedState.code)

        // then
        assertThat(resultCountry).isEqualTo(country.toAppModel())
        assertThat(resultState).isEqualTo(AmbiguousLocation.Defined(associatedState.toAppModel()))
    }

    @Test
    fun `should provide country and state with location code only if state is not found in database`() {
        // given
        val country = WCLocationModel().apply {
            code = "US"
            name = "United States"
        }
        val nonExistentStateLocationCode = "AABBCC"
        whenever(locationStore.getCountries()).thenReturn(listOf(country))
        whenever(locationStore.getStates(country.code)).thenReturn(emptyList())

        // when
        val (resultCountry, resultState) = sut.invoke(country.code, nonExistentStateLocationCode)

        // then
        assertThat(resultCountry).isEqualTo(country.toAppModel())
        assertThat(resultState).isEqualTo(AmbiguousLocation.Raw(nonExistentStateLocationCode))
    }

    @Test
    fun `should provide country and state with location codes only if data not found in database`() {
        // given
        val nonExistentCountryLocationCode = "ZZXXYY"
        val nonExistentStateLocationCode = "AABBCC"
        whenever(locationStore.getCountries()).thenReturn(emptyList())
        whenever(locationStore.getStates(nonExistentCountryLocationCode)).thenReturn(emptyList())

        // when
        val (resultCountry, resultState) = sut.invoke(nonExistentCountryLocationCode, nonExistentStateLocationCode)

        // then
        assertThat(resultCountry).isEqualTo(
            Location(
                code = nonExistentCountryLocationCode,
                name = nonExistentCountryLocationCode
            )
        )
        assertThat(resultState).isEqualTo(AmbiguousLocation.Raw(nonExistentStateLocationCode))
    }
}
