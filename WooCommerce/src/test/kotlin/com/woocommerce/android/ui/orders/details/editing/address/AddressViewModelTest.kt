package com.woocommerce.android.ui.orders.details.editing.address

import androidx.lifecycle.Observer
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressSelectionState
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.BILLING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.AddressType.SHIPPING
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.Field
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ShowCountrySelector
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ShowStateSelector
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.ViewState
import com.woocommerce.android.ui.orders.shippinglabels.creation.CreateShippingLabelTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.data.WCLocationModel
import org.wordpress.android.fluxc.store.WCDataStore

@ExperimentalCoroutinesApi
class AddressViewModelTest : BaseUnitTest() {
    private val savedStateHandle: SavedStateHandle = SavedStateHandle()
    private val selectedSite: SelectedSite = mock()
    private val newCountry = WCLocationModel().apply {
        name = "Brazil"
        code = "BR"
    }

    private val newState = WCLocationModel().apply {
        name = "Acre"
        code = "AC"
        parentCode = "BR"
    }

    private val newCountryWithoutStates = WCLocationModel().apply {
        name = "Country without states"
        code = "123"
    }

    private val dataStore: WCDataStore = mock {
        on { getCountries() } doReturn listOf(newCountry, newCountryWithoutStates)
        on { getStates(newCountry.code) } doReturn listOf(newState)
    }

    private lateinit var addressViewModel: AddressViewModel

    private val viewStateObserver: Observer<ViewState> = mock()
    private val shippingAddress = CreateShippingLabelTestUtils.generateAddress().copy(
        country = testCountry,
        state = testState
    )

    @Before
    fun setup() {
        addressViewModel = AddressViewModel(
            savedStateHandle,
            selectedSite,
            dataStore,
            GetLocations(dataStore)
        )
        addressViewModel.viewStateData.liveData.observeForever(viewStateObserver)
        addressViewModel.shouldEnableDoneButton.observeForever(mock())
    }

    @Test
    fun `Should fetch countries and states on start if they've never been fetched`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        testBlocking {
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            verify(dataStore).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should NOT execute start more than once`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        testBlocking {
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            verify(dataStore).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should execute start again after onScreenDetached is called`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        testBlocking {
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            addressViewModel.onScreenDetached()
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            verify(dataStore, times(0)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should reset view state when onScreenDetached is called`() {
        addressViewModel.onScreenDetached()
        // Default view state is first emitted in the test setup (observeForever) and again on onScreenDetached.
        // That's why we're verifying if default view state was emitted 2 times.
        verify(viewStateObserver, times(2)).onChanged(ViewState())
    }

    @Test
    fun `Should NOT fetch countries and states on start if countries have already been fetched`() {
        testBlocking {
            addressViewModel.start(
                mapOf(SHIPPING to shippingAddress)
            )
            verify(dataStore, times(0)).fetchCountriesAndStates(selectedSite.get())
        }
    }

    @Test
    fun `Should apply country and state changes to view state safely on start if countries list is empty`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        addressViewModel.start(
            mapOf(SHIPPING to shippingAddress)
        )
        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                addressSelectionStates = mapOf(
                    SHIPPING to AddressSelectionState(
                        address = shippingAddress,
                        stateSpinnerStatus = StateSpinnerStatus.RAW_VALUE,
                    )
                ),
            )
        )
    }

    @Test
    fun `Should update viewState with selected country, reset state and enable state selection on country selection`() {
        addressViewModel.start(mapOf(SHIPPING to shippingAddress.copy(country = Location.EMPTY)))
        addressViewModel.onCountrySelected(SHIPPING, newCountry.code)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                addressSelectionStates = mapOf(
                    SHIPPING to AddressSelectionState(
                        address = shippingAddress.copy(
                            country = newCountry.toAppModel(),
                            state = AmbiguousLocation.EMPTY,
                        ),
                        stateSpinnerStatus = StateSpinnerStatus.HAVING_LOCATIONS,
                    )
                ),
            )
        )
    }

    @Test
    fun `Should update viewState with country code instead of name if country code is NOT on countries list`() {
        whenever(dataStore.getCountries()).thenReturn(emptyList())
        val missingCountryCode = "countryCode"

        addressViewModel.start(mapOf(SHIPPING to shippingAddress))
        addressViewModel.onCountrySelected(SHIPPING, missingCountryCode)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                addressSelectionStates = mapOf(
                    SHIPPING to AddressSelectionState(
                        address = shippingAddress.copy(
                            country = Location(missingCountryCode, missingCountryCode),
                            state = AmbiguousLocation.EMPTY
                        ),
                        stateSpinnerStatus = StateSpinnerStatus.RAW_VALUE,
                    ),
                ),
            )
        )
    }

    @Test
    fun `Should update viewState with selected state on state selection`() {
        addressViewModel.start(mapOf(SHIPPING to shippingAddress))

        addressViewModel.onCountrySelected(SHIPPING, newCountry.code)
        addressViewModel.onStateSelected(SHIPPING, newState.code)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                addressSelectionStates = mapOf(
                    SHIPPING to AddressSelectionState(
                        address = shippingAddress.copy(
                            country = newCountry.toAppModel(),
                            state = AmbiguousLocation.Defined(newState.toAppModel())
                        ),
                        stateSpinnerStatus = StateSpinnerStatus.HAVING_LOCATIONS,
                    )
                ),
            )
        )
    }

    @Test
    fun `Should update viewState with state code instead of name if state code is NOT on states list`() {
        whenever(dataStore.getStates(any())).thenReturn(emptyList())
        val stateCode = "stateCode"

        addressViewModel.start(mapOf(SHIPPING to shippingAddress))
        addressViewModel.onStateSelected(SHIPPING, stateCode)

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                addressSelectionStates = mapOf(
                    SHIPPING to AddressSelectionState(
                        address = shippingAddress.copy(state = AmbiguousLocation.Raw(stateCode)),
                        stateSpinnerStatus = StateSpinnerStatus.RAW_VALUE,
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
                listOf(newCountry.toAppModel(), newCountryWithoutStates.toAppModel())
            )
        )
    }

    @Test
    fun `Should trigger state selection event if state selector clicked`() {
        addressViewModel.start(mapOf(SHIPPING to shippingAddress))
        addressViewModel.onCountrySelected(SHIPPING, countryCode = newCountry.code)
        addressViewModel.onStateSpinnerClicked(SHIPPING)

        assertThat(addressViewModel.event.value).isEqualTo(ShowStateSelector(SHIPPING, listOf(newState.toAppModel())))
    }

    @Test
    fun `Should show done button if billing address has been edited`() {
        addressViewModel.start(mapOf(SHIPPING to shippingAddress, BILLING to shippingAddress))

        assertThat(addressViewModel.shouldEnableDoneButton.value).isFalse
        addressViewModel.onFieldEdited(BILLING, Field.FirstName, "new first name")
        assertThat(addressViewModel.shouldEnableDoneButton.value).isTrue
    }

    @Test
    fun `Should show done button if shipping address has been edited`() {
        addressViewModel.start(mapOf(SHIPPING to shippingAddress, BILLING to shippingAddress))

        assertThat(addressViewModel.shouldEnableDoneButton.value).isFalse
        addressViewModel.onFieldEdited(SHIPPING, Field.FirstName, "new first name")
        assertThat(addressViewModel.shouldEnableDoneButton.value).isTrue
    }

    @Test
    fun `Should show done button if adding different shipping address has been disabled`() {
        addressViewModel.start(
            mapOf(
                SHIPPING to shippingAddress.copy(firstName = "Different shipping"),
                BILLING to shippingAddress.copy(firstName = "Different billing")
            )
        )

        assertThat(addressViewModel.shouldEnableDoneButton.value).isFalse
        addressViewModel.onDifferentShippingAddressChecked(false)
        assertThat(addressViewModel.shouldEnableDoneButton.value).isTrue
    }

    @Test
    fun `when clearSelectedAddress, then initialise with empty address`() = testBlocking {
        addressViewModel.clearSelectedAddress()

        assertThat(addressViewModel.viewStateData.liveData.value).isEqualTo(
            ViewState(
                customerId = null,
                addressSelectionStates = mapOf(
                    BILLING to AddressSelectionState(
                        address = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    ),
                    SHIPPING to AddressSelectionState(
                        address = Address.EMPTY,
                        stateSpinnerStatus = StateSpinnerStatus.DISABLED,
                    )
                ),
            )
        )
    }

    @Test
    fun `when onDeleteCustomerClicked, then emit DeleteCustomer event`() {
        addressViewModel.onDeleteCustomerClicked()

        assertThat(addressViewModel.event.value).isEqualTo(AddressViewModel.DeleteCustomer)
    }
}
