package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus.DISABLED
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus.HAVING_LOCATIONS
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel.StateSpinnerStatus.RAW_VALUE
import com.woocommerce.android.util.StringUtils
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
    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    private fun statesAvailableFor(type: AddressType): List<Location> {
        return viewState.addressSelectionStates[type]?.address?.country?.code?.let { locationCode ->
            dataStore.getStates(locationCode)
                .map { it.toAppModel() }
        }.orEmpty()
    }

    private fun statesFor(countryCode: LocationCode): List<Location> {
        return dataStore.getStates(countryCode)
            .map { it.toAppModel() }
    }

    private var hasStarted = false
    private var initialState = emptyMap<AddressType, Address>()

    val isAnyAddressEdited: LiveData<Boolean> = viewStateData.liveData.map { viewState ->
        viewState.addressSelectionStates.isNotEmpty() &&
            viewState.addressSelectionStates.mapValues { it.value.address } != initialState
    }

    private val _isDifferentShippingAddressChecked = MutableLiveData<Boolean>()
    val isDifferentShippingAddressChecked: LiveData<Boolean> = _isDifferentShippingAddressChecked

    val shouldEnableDoneButton = isAnyAddressEdited.combineWith(
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
                        stateSpinnerStatus = getStateSpinnerStatus(
                            initialSingleAddressState.value.country.code
                        )
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

    private fun getStateSpinnerStatus(countryCode: String): StateSpinnerStatus {
        return when {
            countryCode.isBlank() -> DISABLED
            statesFor(countryCode).isNotEmpty() -> HAVING_LOCATIONS
            else -> RAW_VALUE
        }
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
                        stateSpinnerStatus = getStateSpinnerStatus(countryCode)
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

    fun onDeleteCustomerClicked() {
        triggerEvent(DeleteCustomer)
    }

    fun onDoneSelected(addDifferentShippingChecked: Boolean? = null) {
        // order creation/editing will fail if billing email address is invalid
        viewState.addressSelectionStates.get(AddressType.BILLING)?.address?.email?.let { billingEmail ->
            if (billingEmail.isNotEmpty() && !StringUtils.isValidEmail(billingEmail)) {
                triggerEvent(MultiLiveEvent.Event.ShowSnackbar(R.string.email_invalid))
                return
            }
        }

        triggerEvent(
            Exit(
                customerId = viewState.customerId,
                firstName = viewState.firstName,
                lastName = viewState.lastName,
                email = viewState.email,
                addresses = viewState.addressSelectionStates.mapValues { statePair ->
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
            Field.Email -> currentAddress.copy(email = value.trim())
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

    fun onAddressesChanged(customer: Order.Customer) {
        hasStarted = true
        viewState = viewState.copy(
            customerId = customer.customerId,
            firstName = customer.firstName,
            lastName = customer.lastName,
            email = customer.email,
            addressSelectionStates = mapOf(
                AddressType.BILLING to AddressSelectionState(
                    customer.billingAddress,
                    getStateSpinnerStatus(customer.billingAddress.country.code)
                ),
                AddressType.SHIPPING to AddressSelectionState(
                    customer.shippingAddress,
                    getStateSpinnerStatus(customer.shippingAddress.country.code)
                )
            )
        )
    }

    fun clearSelectedAddress() {
        hasStarted = true
        this.initialState = mapOf(
            AddressType.BILLING to Address.EMPTY,
            AddressType.SHIPPING to Address.EMPTY,
        )
        viewState = ViewState()
        initialize(initialState)
    }

    @Parcelize
    data class ViewState(
        val customerId: Long? = null,
        val firstName: String? = null,
        val lastName: String? = null,
        val email: String? = null,
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
        val customerId: Long?,
        val firstName: String?,
        val lastName: String?,
        val email: String?,
        val addresses: Map<AddressType, Address>
    ) : MultiLiveEvent.Event()

    object DeleteCustomer : MultiLiveEvent.Event()

    enum class Field {
        FirstName, LastName, Company, Phone, Address1, Address2, City, State, Zip, Email
    }
}
