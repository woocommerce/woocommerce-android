package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
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
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    private fun statesAvailableFor(type: AddressType): List<Location> {
        val selectedCountry = selectedCountryLocationFor(type)
        return dataStore.getStates(selectedCountry.code)
            .map { it.toAppModel() }
    }

    private fun statesFor(countryCode: LocationCode): List<Location> {
        return dataStore.getStates(countryCode)
            .map { it.toAppModel() }
    }

    fun selectedCountryLocationFor(type: AddressType) =
        viewState.countryStatePairs.getValue(type).countryLocation

    fun selectedStateLocationFor(type: AddressType) =
        viewState.countryStatePairs.getValue(type).stateLocation

    private var hasStarted = false

    /**
     * The start method is called when the view is created. When the view is recreated (e.g. navigating to country
     * search and back) we don't want this method to be called again, otherwise the ViewModel will replace the newly
     * selected country or state with the previously saved values.
     */
    fun start(countryCode: LocationCode, stateCode: LocationCode) {
        if (hasStarted) {
            return
        }
        hasStarted = true
        initialize(countryCode, stateCode)
    }

    private fun initialize(countryCode: String, stateCode: String) {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState = viewState.copy(isLoading = false)
            }
            initializeCountriesAndStates(countryCode, stateCode)
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

    private fun getCountryNameFromCode(countryCode: LocationCode): String =
        countries.find { it.code == countryCode }?.name ?: countryCode

    private fun getCountryLocationFromCode(countryCode: LocationCode) =
        Location(countryCode, getCountryNameFromCode(countryCode))

    private fun getStateLocationFromCode(countryCode: LocationCode, stateCode: LocationCode) = Location(
        code = stateCode,
        name = dataStore.getStates(countryCode).firstOrNull { state -> state.code == stateCode }?.name ?: stateCode
    )

    fun onCountrySelected(type: AddressType, countryCode: LocationCode) {
        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
                if (entry.key == type) {
                    entry.value.copy(
                        countryLocation = getCountryLocationFromCode(countryCode),
                        stateLocation = Location.EMPTY,
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
            isStateSelectionEnabled = true
        )
    }

    fun onViewDestroyed(currentFormsState: Map<AddressType, Address>) {
        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
                entry.value.copy(
                    inputFormValues = currentFormsState.getValue(entry.key)
                )
            }
        )
    }

    fun onStateSelected(type: AddressType, stateCode: LocationCode) {
        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.mapValues { entry ->
                if (entry.key == type) {
                    entry.value.copy(
                        stateLocation = getStateLocationFromCode(entry.value.countryLocation.code, stateCode)
                    )
                } else {
                    entry.value
                }
            }
        )
    }

    private fun initializeCountriesAndStates(countryCode: LocationCode, stateCode: LocationCode) {
        // TODO handle correct initialization in #5290
        viewState = viewState.copy(
            countryStatePairs = mapOf(
                AddressType.BILLING to AddressSelectionState(
                    countryLocation = getCountryLocationFromCode(countryCode),
                    stateLocation = getStateLocationFromCode(countryCode, stateCode),
                    inputFormValues = Address.EMPTY,
                    stateSpinnerStatus = StateSpinnerStatus.DISABLED
                ),
                AddressType.SHIPPING to AddressSelectionState(
                    countryLocation = getCountryLocationFromCode(countryCode),
                    stateLocation = getStateLocationFromCode(countryCode, stateCode),
                    inputFormValues = Address.EMPTY,
                    stateSpinnerStatus = StateSpinnerStatus.DISABLED
                )
            ),
            isStateSelectionEnabled = countryCode.isNotEmpty()
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

    @Parcelize
    data class ViewState(
        val countryStatePairs: Map<AddressType, AddressSelectionState> = mapOf(
            AddressType.BILLING to AddressSelectionState(
                inputFormValues = Address.EMPTY,
                countryLocation = Location.EMPTY,
                stateLocation = Location.EMPTY,
                stateSpinnerStatus = StateSpinnerStatus.DISABLED,
            ),
            AddressType.SHIPPING to AddressSelectionState(
                inputFormValues = Address.EMPTY,
                countryLocation = Location.EMPTY,
                stateLocation = Location.EMPTY,
                stateSpinnerStatus = StateSpinnerStatus.DISABLED,
            ),
        ),
        val isLoading: Boolean = false,
        @Deprecated("Use stateSpinnerStatus of corresponding AddressSelectionState")
        val isStateSelectionEnabled: Boolean = false
    ) : Parcelable

    enum class AddressType {
        SHIPPING, BILLING
    }

    enum class StateSpinnerStatus {
        HAVING_LOCATIONS, RAW_VALUE, DISABLED
    }

    @Parcelize
    data class AddressSelectionState(
        val inputFormValues: Address,
        val countryLocation: Location,
        val stateLocation: Location,
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
}
