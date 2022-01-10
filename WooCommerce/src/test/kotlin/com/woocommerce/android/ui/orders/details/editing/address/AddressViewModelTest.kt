package com.woocommerce.android.ui.orders.details.editing.address

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.*
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.BILLING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.SHIPPING
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class AddressViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val selectedSite: SelectedSite = mock()
    private val country = WCLocationModel().apply {
        name = "Brazil"
        code = "BR"
    }

    private val state = WCLocationModel().apply {
        name = "Acre"
        code = "AC"
        parentCode = "BR"
    }

    private val countryWithoutStates = WCLocationModel().apply {
        name = "Country without states"
        code = "123"
    }

    private val dataStore: WCDataStore = mock {
        on { getCountries() } doReturn listOf(country, countryWithoutStates)
        on { getStates(country.code) } doReturn listOf(state)
    }

    private val addressViewModel = AddressViewModel(savedStateHandle, selectedSite, dataStore)
    private val viewStateObserver: Observer<ViewState> = mock()
    private val addressType = SHIPPING

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
        addressViewModel.onCountrySelected(addressType, country.code)
        addressViewModel.onScreenDetached()
        // Default view state is first emitted in the test setup (observeForever) and again on onScreenDetached.
        // That's why we're verifying if default view state was emitted 2 times.
        verify(viewStateObserver, times(2)).onChanged(ViewState())
    }

    @Test
    fun `Should NOT fetch countries and states on start if countries have already been fetched`() {
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
                    BILLING to AddressSelectionState(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location(stateCode, stateCode),
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                    SHIPPING to AddressSelectionState(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location(stateCode, stateCode),
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    )
                ),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should return true on hasStates if states list is NOT empty`() {
        whenever(dataStore.getStates(any())).thenReturn(listOf(country))
        assertTrue(addressViewModel.hasStatesFor(addressType))
    }

    @Test
    fun `Should return false on hasStates if states list is empty`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        assertFalse(addressViewModel.hasStatesFor(addressType))
    }

    @Test
    fun `Should update viewState with selected country, reset state and enable state selection on country selection`() {
        addressViewModel.onCountrySelected(SHIPPING, country.code)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                countryStatePairs = mapOf(
                    BILLING to AddressSelectionState(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                    SHIPPING to AddressSelectionState(
                        countryLocation = Location(country.code, country.name),
                        stateLocation = Location("", ""),
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.HAVING_LOCATIONS,
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
        addressViewModel.onCountrySelected(BILLING, countryCode)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                countryStatePairs = mapOf(
                    BILLING to AddressSelectionState(
                        countryLocation = Location(countryCode, countryCode),
                        stateLocation = Location.EMPTY,
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.RAW_VALUE,
                    ),
                    SHIPPING to AddressSelectionState(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                ),
                isStateSelectionEnabled = true
            )
        )
    }

    @Test
    fun `Should update viewState with selected state on state selection`() {
        addressViewModel.onCountrySelected(SHIPPING, country.code)
        addressViewModel.onStateSelected(SHIPPING, state.code)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                countryStatePairs = mapOf(
                    BILLING to AddressSelectionState(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                    SHIPPING to AddressSelectionState(
                        countryLocation = Location(country.code, country.name),
                        stateLocation = Location(state.code, state.name),
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.HAVING_LOCATIONS,
                    )
                ),
                isStateSelectionEnabled = true
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
                    BILLING to AddressSelectionState(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location.EMPTY,
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                    SHIPPING to AddressSelectionState(
                        countryLocation = Location.EMPTY,
                        stateLocation = Location(stateCode, stateCode),
                        inputFormValues = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    )
                ),
            )
        )
    }

    @Test
    fun `Should trigger country selection event if country selector clicked`() {
        addressViewModel.onCountrySpinnerClicked(SHIPPING)

        assertThat(addressViewModel.event.value).isEqualTo(
            ShowCountrySelector(
                SHIPPING,
                listOf(country.toAppModel(), countryWithoutStates.toAppModel())
            )
        )
    }

    @Test
    fun `Should trigger state selection event if state selector clicked`() {
        addressViewModel.onCountrySelected(SHIPPING, countryCode = country.code)
        addressViewModel.onStateSpinnerClicked(SHIPPING)

        assertThat(addressViewModel.event.value).isEqualTo(ShowStateSelector(SHIPPING, listOf(state.toAppModel())))
    }

    @Test
    fun `Should prepare rich billing address if country and state are selected from predefined values`() {
        // when
        addressViewModel.onCountrySelected(BILLING, countryCode = country.code)
        addressViewModel.onStateSelected(BILLING, stateCode = state.code)
        addressViewModel.onDoneSelected(
            currentFormsState = mapOf(
                BILLING to onlyInputFieldsValues,
                SHIPPING to Address.EMPTY
            )
        )

        // then
        val expectedAddress = onlyInputFieldsValues.copy(country = country.name, state = state.name)
        assertThat(addressViewModel.event.value).isEqualTo(
            Exit(
                billingAddress = expectedAddress,
                shippingAddress = expectedAddress
            )
        )
    }

    @Test
    fun `Should prepare rich billing address if state is plain text`() {
        // given
        val inputFieldsWithState = onlyInputFieldsValues.copy(state = "plain text state")

        // when
        addressViewModel.onCountrySelected(BILLING, countryCode = countryWithoutStates.code)
        addressViewModel.onDoneSelected(
            currentFormsState = mapOf(
                BILLING to inputFieldsWithState,
                SHIPPING to Address.EMPTY
            )
        )

        // then
        val expectedAddress = onlyInputFieldsValues.copy(
            country = countryWithoutStates.name,
            state = inputFieldsWithState.state
        )
        assertThat(addressViewModel.event.value).isEqualTo(
            Exit(
                billingAddress = expectedAddress,
                shippingAddress = expectedAddress
            )
        )
    }

    companion object {
        val onlyInputFieldsValues = Address(
            "Company",
            "First name",
            "Last name",
            "Phone",
            country = "",
            state = "",
            "Address1",
            "Address2",
            "City",
            "Postcode",
            "Email"
        )
    }
}
