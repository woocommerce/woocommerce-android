package com.woocommerce.android.ui.orders.details.editing.address

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Location
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ViewState
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddressViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val selectedSite: SelectedSite = mock()
    private val dataStore: WCDataStore = mock()

    private val location = WCLocationModel().apply {
        name = "Brazil"
        code = "BR"
    }
    private val addressViewModel = AddressViewModel(savedStateHandle, selectedSite, dataStore)
    private val viewStateObserver: Observer<ViewState> = mock()
    private val addressType = AddressViewModel.AddressType.SHIPPING

    @Before
    fun setup() {
        addressViewModel.viewStateData.liveData.observeForever(viewStateObserver)
    }

    @Test
    fun `Should fetch countries and states on start if they've never been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            verify(dataStore).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should NOT execute start more than once`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            verify(dataStore).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should execute start again after onScreenDetached is called`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            addressViewModel.onScreenDetached()
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            verify(dataStore, times(2)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should reset view state when onScreenDetached is called`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        val countryCode = location.code
        addressViewModel.onCountrySelected(addressType, countryCode)
        addressViewModel.onScreenDetached()
        // Default view state is first emitted in the test setup (observeForever) and again on onScreenDetached.
        // That's why we're verifying if default view state was emitted 2 times.
        verify(viewStateObserver, times(2)).onChanged(ViewState())
    }

    @Test
    fun `Should NOT fetch countries and states on start if countries have already been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                countryCode = "countryCode",
                stateCode = "stateCode"
            )
            verify(dataStore, times(0)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should apply country and state changes to view state safely on start if countries list is empty`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        val countryCode = "countryCode"
        val stateCode = "stateCode"
        addressViewModel.start(countryCode, stateCode)
        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                countryStatePairs = mapOf(
                    AddressViewModel.AddressType.BILLING to AddressViewModel.CountryStatePair(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location(stateCode, stateCode),
                    ),
                    AddressViewModel.AddressType.SHIPPING to AddressViewModel.CountryStatePair(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location(stateCode, stateCode),
                    )
                ),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should return true on hasCountries if countries list is NOT empty`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        assertTrue(addressViewModel.hasCountries())
    }

    @Test
    fun `Should return false on hasCountries if countries list is empty`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        assertFalse(addressViewModel.hasCountries())
    }

    @Test
    fun `Should return true on hasStates if states list is NOT empty`() {
        whenever(dataStore.getStates(any())).thenReturn(listOf(location))
        assertTrue(addressViewModel.hasStatesFor(addressType))
    }

    @Test
    fun `Should return false on hasStates if states list is empty`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        assertFalse(addressViewModel.hasStatesFor(addressType))
    }

    @Test
    fun `Should update viewState with selected country, reset state and enable state selection on onCountrySelected`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        val countryCode = location.code
        addressViewModel.onCountrySelected(AddressViewModel.AddressType.SHIPPING, countryCode)
        verify(viewStateObserver).onChanged(
            ViewState(
                countryStatePairs = mapOf(
                    AddressViewModel.AddressType.BILLING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                    ),
                    AddressViewModel.AddressType.SHIPPING to AddressViewModel.CountryStatePair(
                        countryLocation = Location(countryCode, location.name),
                        stateLocation = Location("", ""),
                    )
                ),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should update viewState with country code instead of name if country code is NOT on countries list`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        val countryCode = "countryCode"
        addressViewModel.onCountrySelected(AddressViewModel.AddressType.BILLING, countryCode)
        verify(viewStateObserver).onChanged(
            ViewState(
                countryStatePairs = mapOf(
                    AddressViewModel.AddressType.BILLING to AddressViewModel.CountryStatePair(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location.EMPTY,
                    ),
                    AddressViewModel.AddressType.SHIPPING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                    ),
                ),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should update viewState with selected state on onCountrySelected`() {
        whenever(dataStore.getStates(any())).thenReturn(listOf(location))
        val stateCode = location.code
        addressViewModel.onStateSelected(AddressViewModel.AddressType.SHIPPING, stateCode)
        verify(viewStateObserver).onChanged(
            ViewState(
                countryStatePairs = mapOf(
                    AddressViewModel.AddressType.BILLING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                    ),
                    AddressViewModel.AddressType.SHIPPING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location(stateCode, location.name),
                    )
                ),
            )
        )
    }

    @Test
    fun `Should update viewState with state code instead of name if state code is NOT on states list`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        val stateCode = "stateCode"
        addressViewModel.onStateSelected(addressType, stateCode)
        verify(viewStateObserver).onChanged(
            ViewState(
                countryStatePairs = mapOf(
                    AddressViewModel.AddressType.BILLING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                    ),
                    AddressViewModel.AddressType.SHIPPING to AddressViewModel.CountryStatePair(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location(stateCode, stateCode),
                    )
                ),
            )
        )
    }
}
