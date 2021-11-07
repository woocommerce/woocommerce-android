package com.woocommerce.android.ui.orders.details.editing.address

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
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

    private val storedAddress = Address(
        firstName = "first name stored",
        lastName = "last name stored",
        company = "Company stored",
        phone = "phone stored",
        address1 = "Address 1 stored",
        address2 = "address 2 stored",
        city = "City stored",
        postcode = "postcode stored",
        email = "email stored",
        country = "country stored",
        state = "state stored"
    )

    private val addressDraft = Address(
        firstName = "first name draft",
        lastName = "last name draft",
        company = "Company draft",
        phone = "phone draft",
        address1 = "Address 1 draft",
        address2 = "address 2 draft",
        city = "City draft",
        postcode = "postcode draft",
        email = "email draft",
        country = "country draft",
        state = "state draft"
    )

    @Before
    fun setup() {
        addressViewModel.viewStateData.liveData.observeForever(viewStateObserver)
    }

    @Test
    fun `Should fetch countries and states on start if they've never been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                storedAddress = storedAddress,
                addressDraft = addressDraft
            )
            verify(dataStore, times(1)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should NOT fetch countries and states on start if countries have already been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(listOf(location))
        coroutinesTestRule.testDispatcher.runBlockingTest {
            addressViewModel.start(
                storedAddress = storedAddress,
                addressDraft = addressDraft
            )
            verify(dataStore, times(0)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should apply country and state changes to view state safely on start if countries list is empty`() {
        addressViewModel.start(
            storedAddress = storedAddress,
            addressDraft = addressDraft
        )
        assertThat(addressViewModel.viewStateData.liveData.value?.isStateSelectionEnabled).isEqualTo(true)
    }

    @Test
    fun `Should update view state with address draft country if it's not empty`() {
        addressViewModel.start(
            storedAddress = storedAddress,
            addressDraft = addressDraft
        )
        assertThat(addressViewModel.viewStateData.liveData.value?.countryLocation).isEqualTo(
            Location(
                addressDraft.country,
                addressDraft.country
            )
        )
    }

    @Test
    fun `Should update view state with address draft state if it's not empty`() {
        addressViewModel.start(
            storedAddress = storedAddress,
            addressDraft = addressDraft
        )
        assertThat(addressViewModel.viewStateData.liveData.value?.stateLocation).isEqualTo(
            Location(
                addressDraft.state,
                addressDraft.state
            )
        )
    }

    @Test
    fun `Should update view state with stored address country if address draft country is empty`() {
        val emptyCountryAndStateAddress = addressDraft.copy(country = "", state = "")
        addressViewModel.start(
            storedAddress = storedAddress,
            addressDraft = emptyCountryAndStateAddress
        )
        assertThat(addressViewModel.viewStateData.liveData.value?.countryLocation).isEqualTo(
            Location(
                storedAddress.country,
                storedAddress.country
            )
        )
    }

    @Test
    fun `Should update view state with stored address state if address draft state is empty`() {
        val emptyCountryAndStateAddress = addressDraft.copy(country = "", state = "")
        addressViewModel.start(
            storedAddress = storedAddress,
            addressDraft = emptyCountryAndStateAddress
        )
        assertThat(addressViewModel.viewStateData.liveData.value?.stateLocation).isEqualTo(
            Location(
                storedAddress.state,
                storedAddress.state
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
