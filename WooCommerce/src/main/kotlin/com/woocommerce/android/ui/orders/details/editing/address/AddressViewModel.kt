package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.*
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
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
        return dataStore.getStates(viewState.countryStatePairs.getValue(type).address.country.code)
            .map { it.toAppModel() }
    }

    private fun statesFor(countryCode: LocationCode): List<Location> {
        return dataStore.getStates(countryCode)
            .map { it.toAppModel() }
    }

    private var hasStarted = false

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
        initialize(initialState)
    }

    private fun initialize(initialState: Map<AddressType, Address>) {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState = viewState.copy(isLoading = false)
            }
            initializeCountriesAndStates(initialState)
        }
    }

    @Deprecated("Use stateSpinnerStatus of corresponding AddressSelectionState")
    fun hasStatesFor(type: AddressType) = statesAvailableFor(type).isNotEmpty()

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
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
                if (entry.key == type) {
                    entry.value.copy(
                        address = entry.value.address.copy(
                            country = selectedCountry,
                            state = AmbiguousLocation.EMPTY
                        ),
                        stateSpinnerStatus = if (statesFor(countryCode).isEmpty()) {
                            StateSpinnerStatus.RAW_VALUE
                        } else {
                            StateSpinnerStatus.HAVING_LOCATIONS
                        }
                    )
                } else {
                    entry.value
                }
            },
        )
    }

    fun onViewDestroyed(currentFormsState: Map<AddressType, Address>) {
        updateInputFormValues(currentFormsState)
    }

    private fun updateInputFormValues(currentFormsState: Map<AddressType, Address>) {
        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
                entry.value.copy(
                    address = currentFormsState.getValue(entry.key)
                )
            }
        )
    }

    fun onStateSelected(type: AddressType, selectedStateCode: LocationCode) {
        val (_, selectedState) = getLocations(
            countryCode = viewState.countryStatePairs.getValue(type).address.country.code,
            stateCode = selectedStateCode,
        )

        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
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

    private fun initializeCountriesAndStates(initialState: Map<AddressType, Address>) {
        viewState = viewState.copy(
            countryStatePairs = initialState.mapValues {
                AddressSelectionState(
                    address = it.value,
                    stateSpinnerStatus = if (statesFor(it.value.country.code).isEmpty()) {
                        StateSpinnerStatus.RAW_VALUE
                    } else {
                        StateSpinnerStatus.HAVING_LOCATIONS
                    }
                )
            },
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

    @Suppress("UnusedPrivateMember")
    fun onDoneSelected(currentFormsState: Map<AddressType, Address>) {
        // no-op
    }

    @Parcelize
    data class ViewState(
        val countryStatePairs: Map<AddressType, AddressSelectionState> = emptyMap(),
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

    // TODO return map of addresses
    data class Exit(
        val billingAddress: Address,
        val shippingAddress: Address
    ) : MultiLiveEvent.Event()
}
