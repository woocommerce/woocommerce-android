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

    private val location = WCLocationModel().apply {
        name = "Brazil"
        code = "BR"
    }

    private val dataStore: WCDataStore = mock()

    private val addressViewModel = AddressViewModel(savedStateHandle, selectedSite, dataStore)

    private val viewStateObserver: Observer<ViewState> = mock()

    @Before
    fun setup() {
        addressViewModel.viewStateData.liveData.observeForever(viewStateObserver)
    }

    @Test
    fun `Should fetch countries and states on start if they've never been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start("country", "state")
            verify(dataStore, times(1)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should NOT fetch countries and states on start if countries have already been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start("country", "state")
            verify(dataStore, times(0)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should apply country and state changes to view state safely on start if countries list is empty`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        val countryCode = "countryCode"
        val stateCode = "stateCode"
        addressViewModel.start(countryCode, stateCode)
        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                countryLocation = Location(countryCode, countryCode),
                stateLocation = Location(stateCode, stateCode),
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
        assertTrue(addressViewModel.hasStates())
    }

    @Test
    fun `Should return false on hasStates if states list is empty`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        assertFalse(addressViewModel.hasStates())
    }

    @Test
    fun `Should update viewState with selected country, reset selected state and enable state selection on onCountrySelected`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        val countryCode = location.code
        addressViewModel.onCountrySelected(countryCode)
        verify(viewStateObserver, times(1)).onChanged(
            ViewState(
                countryLocation = Location(countryCode, location.name),
                stateLocation = Location("", ""),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should update viewState with country code instead of name if country code is NOT on countries list`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        val countryCode = "countryCode"
        addressViewModel.onCountrySelected(countryCode)
        verify(viewStateObserver, times(1)).onChanged(
            ViewState(
                countryLocation = Location(countryCode, countryCode),
                stateLocation = Location("", ""),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should update viewState with selected state on onCountrySelected`() {
        whenever(dataStore.getStates(any())).thenReturn(listOf(location))
        val stateCode = location.code
        addressViewModel.onStateSelected(stateCode)
        verify(viewStateObserver, times(1)).onChanged(
            ViewState(
                stateLocation = Location(stateCode, location.name)
            )
        )
    }

    @Test
    fun `Should update viewState with state code instead of name if state code is NOT on states list`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        val stateCode = "stateCode"
        addressViewModel.onStateSelected(stateCode)
        verify(viewStateObserver, times(1)).onChanged(
            ViewState(
                stateLocation = Location(stateCode, stateCode)
            )
        )
    }
}
