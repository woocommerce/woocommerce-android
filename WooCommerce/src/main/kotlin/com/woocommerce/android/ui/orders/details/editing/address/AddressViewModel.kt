package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.model.*
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus.*
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.combineWith
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

typealias LocationCode = String

@HiltViewModel
class AddressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val dataStore: WCDataStore,
    private val getLocations: GetLocations,
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    private fun statesAvailableFor(type: AddressType): List<Location> {
        return dataStore.getStates(viewState.addressSelectionStates.getValue(type).address.country.code)
            .map { it.toAppModel() }
    }

    private fun statesFor(countryCode: LocationCode): List<Location> {
        return dataStore.getStates(countryCode)
            .map { it.toAppModel() }
    }

    private var hasStarted = false
    private var initialState = emptyMap<AddressType, Address>()

    val isAnyAddressEdited: LiveData<Boolean> = viewStateData.liveData.map { viewState ->
        viewState.addressSelectionStates.mapValues { it.value.address } != initialState
    }

    private val _isDifferentShippingAddressChecked = MutableLiveData<Boolean>()
    val isDifferentShippingAddressChecked: LiveData<Boolean> = _isDifferentShippingAddressChecked

    val shouldShowDoneButton = isAnyAddressEdited.combineWith(
        isDifferentShippingAddressChecked,
        viewStateData.liveData.map { it.addressSelectionStates[AddressType.SHIPPING]?.address }
    ) { isAnyAddressEdited, isDifferentShippingAddressChecked, shippingAddress ->
        val isDifferentShippingAddressDisabled =
            isDifferentShippingAddressChecked == false && (shippingAddress != Address.EMPTY)

        isAnyAddressEdited == true || isDifferentShippingAddressDisabled
    }

    /**
     * The start method is called when the view is created. When the view is recreated (e.g. navigating to country
     * search and back) we don't want this method to be called again, otherwise the ViewModel will replace the newly
     * selected country or state with the previously saved values.
     */
    fun start(initialState: Map<AddressType, Address>) {
        if (hasStarted) {
            return
        }
        hasStarted = true
        this.initialState = initialState
        initialize(initialState)
    }

    private fun initialize(initialState: Map<AddressType, Address>) {
        launch {
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState = viewState.copy(isLoading = false)
            }
            viewState = viewState.copy(
                addressSelectionStates = initialState.mapValues { initialSingleAddressState ->
                    AddressSelectionState(
                        address = initialSingleAddressState.value,
                        stateSpinnerStatus = when {
                            initialSingleAddressState.value.country.code.isBlank() -> DISABLED
                            statesFor(initialSingleAddressState.value.country.code).isNotEmpty() -> HAVING_LOCATIONS
                            else -> RAW_VALUE
                        }
                    )
                }
            )
        }
    }

    fun onDifferentShippingAddressChecked(checked: Boolean) {
        _isDifferentShippingAddressChecked.value = checked
    }

    /**
     * Even when the [BaseAddressEditingFragment] instance is destroyed the instance of [AddressViewModel] will still
     * be alive. That means if the user have selected a country or state, discarded the change and navigated again to
     * [BaseAddressEditingFragment] the discarded values of country and state will be used. Because of that, when the
     * Fragment is detached we must clear these values from the ViewModel. We should also allow the [start] method
     * to be called again.
     */
    fun onScreenDetached() {
        hasStarted = false
        viewState = ViewState()
    }

    fun onCountrySelected(type: AddressType, countryCode: LocationCode) {
        val selectedCountry = dataStore.getCountries().firstOrNull { it.code == countryCode }?.toAppModel()
            ?: Location(countryCode, countryCode)

        viewState = viewState.copy(
            addressSelectionStates = viewState.addressSelectionStates.mapValues { entry ->
                if (entry.key == type) {
                    entry.value.copy(
                        address = entry.value.address.copy(
                            country = selectedCountry,
                            state = AmbiguousLocation.EMPTY
                        ),
                        stateSpinnerStatus = if (statesFor(countryCode).isEmpty()) {
                            RAW_VALUE
                        } else {
                            HAVING_LOCATIONS
                        }
                    )
                } else {
                    entry.value
                }
            },
        )
    }

    fun onStateSelected(type: AddressType, selectedStateCode: LocationCode) {
        val (_, selectedState) = getLocations(
            countryCode = viewState.addressSelectionStates.getValue(type).address.country.code,
            stateCode = selectedStateCode,
        )

        viewState = viewState.copy(
            addressSelectionStates = viewState.addressSelectionStates.mapValues { entry ->
                if (entry.key == type) {
                    entry.value.copy(
                        address = entry.value.address.copy(
                            state = selectedState
                        )
                    )
                } else {
                    entry.value
                }
            }
        )
    }

    fun onCountrySpinnerClicked(type: AddressType) {
        val event = ShowCountrySelector(type, countries)
        triggerEvent(event)
    }

    fun onStateSpinnerClicked(type: AddressType) {
        val event = ShowStateSelector(type, statesAvailableFor(type))
        triggerEvent(event)
    }

    fun onDoneSelected(addDifferentShippingChecked: Boolean? = null) {
        triggerEvent(
            Exit(
                viewState.addressSelectionStates.mapValues { statePair ->
                    if (addDifferentShippingChecked == false && statePair.key == AddressType.SHIPPING) {
                        Address.EMPTY
                    } else {
                        statePair.value.address
                    }
                }
            )
        )
    }

    fun onFieldEdited(
        addressType: AddressType,
        field: Field,
        value: String
    ) {
        val currentAddress = viewState.addressSelectionStates.getValue(addressType).address
        val newAddress = when (field) {
            Field.FirstName -> currentAddress.copy(firstName = value)
            Field.LastName -> currentAddress.copy(lastName = value)
            Field.Company -> currentAddress.copy(company = value)
            Field.Phone -> currentAddress.copy(phone = value)
            Field.Address1 -> currentAddress.copy(address1 = value)
            Field.Address2 -> currentAddress.copy(address2 = value)
            Field.City -> currentAddress.copy(city = value)
            Field.State -> currentAddress.copy(state = AmbiguousLocation.Raw(value))
            Field.Zip -> currentAddress.copy(postcode = value)
            Field.Email -> currentAddress.copy(email = value)
        }
        viewState = viewState.copy(
            addressSelectionStates = viewState.addressSelectionStates.mapValues { entry ->
                if (entry.key == addressType) {
                    entry.value.copy(address = newAddress)
                } else {
                    entry.value
                }
            }
        )
    }

    @Parcelize
    data class ViewState(
        val addressSelectionStates: Map<AddressType, AddressSelectionState> = emptyMap(),
        val isLoading: Boolean = false,
    ) : Parcelable

    enum class AddressType {
        SHIPPING, BILLING
    }

    enum class StateSpinnerStatus {
        HAVING_LOCATIONS, RAW_VALUE, DISABLED
    }

    @Parcelize
    data class AddressSelectionState(
        val address: Address,
        val stateSpinnerStatus: StateSpinnerStatus
    ) : Parcelable

    data class ShowCountrySelector(
        val type: AddressType,
        val countries: List<Location>
    ) : MultiLiveEvent.Event()

    data class ShowStateSelector(
        val type: AddressType,
        val states: List<Location>
    ) : MultiLiveEvent.Event()

    data class Exit(
        val addresses: Map<AddressType, Address>
    ) : MultiLiveEvent.Event()

    enum class Field {
        FirstName, LastName, Company, Phone, Address1, Address2, City, State, Zip, Email
    }
}
