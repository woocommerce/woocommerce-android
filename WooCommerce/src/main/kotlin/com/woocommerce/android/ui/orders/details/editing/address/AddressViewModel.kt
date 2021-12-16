package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.LiveDataDelegate
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

    val countries: List<Location>
        get() = dataStore.getCountries().map { it.toAppModel() }

    fun statesAvailableFor(type: AddressType): List<Location> {
        val selectedCountry = selectedCountryLocationFor(type)
        return dataStore.getStates(selectedCountry.code)
            .map { it.toAppModel() }
    }

    fun selectedCountryLocationFor(type: AddressType): Location {
        return viewState.countryStatePairs.getValue(type).countryLocation
    }

    fun selectedStateLocationFor(type: AddressType): Location {
        return viewState.countryStatePairs.getValue(type).stateLocation
    }

    private var hasStarted = false

    /**
     * The start method is called when the view is created. When the view is recreated (e.g. navigating to country
     * search and back) we don't want this method to be called again, otherwise the ViewModel will replace the newly
     * selected country or state with the previously saved values.
     */
    fun start(countryCode: String, stateCode: String) {
        if (hasStarted) {
            return
        }
        hasStarted = true
        initializeCountriesAndStates(countryCode, stateCode)
    }

    private fun initializeCountriesAndStates(countryCode: String, stateCode: String) {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState = viewState.copy(isLoading = false)
            }
            applyCountryStateChangesSafely(countryCode, stateCode)
        }
    }

    fun hasCountries() = countries.isNotEmpty()

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

    private fun getCountryNameFromCode(countryCode: String): String {
        return countries.find { it.code == countryCode }?.name ?: countryCode
    }

    private fun getCountryLocationFromCode(countryCode: String): Location {
        return Location(countryCode, getCountryNameFromCode(countryCode))
    }

    private fun getStateLocationFromCode(country: String, stateCode: String): Location {
        return Location(
            code = stateCode,
            name = dataStore.getStates(country).firstOrNull { state -> state.code == stateCode }?.name ?: stateCode
        )
    }

    fun onCountrySelected(type: AddressType, countryCode: String) {
        val countryLocation = getCountryLocationFromCode(countryCode)
        viewState = viewState.copy(
            countryStatePairs = viewState.countryStatePairs.toMutableMap().apply {
                put(
                    type,
                    CountryStatePair(
                        countryLocation = countryLocation,
                        stateLocation = Location.EMPTY
                    )
                )
            },
            isStateSelectionEnabled = true
        )

        println()
    }

    fun onStateSelected(type: AddressType, stateCode: String) {
        val initialLocation = viewState.countryStatePairs.getValue(type)
        if (stateCode != initialLocation.stateLocation.code) {
            viewState = viewState.copy(
                countryStatePairs = viewState.countryStatePairs.toMutableMap().apply {
                    put(
                        type,
                        initialLocation.copy(
                            stateLocation = getStateLocationFromCode(initialLocation.countryLocation.code, stateCode)
                        )
                    )
                }
            )
        }
    }

    private fun applyCountryStateChangesSafely(countryCode: String, stateCode: String) {
        viewState = viewState.copy(
            countryStatePairs = mapOf(
                AddressType.BILLING to CountryStatePair(
                    countryLocation = getCountryLocationFromCode(countryCode),
                    stateLocation = getStateLocationFromCode(countryCode, stateCode)
                ),
                AddressType.SHIPPING to CountryStatePair(
                    countryLocation = getCountryLocationFromCode(countryCode),
                    stateLocation = getStateLocationFromCode(countryCode, stateCode)
                )
            ),
            isStateSelectionEnabled = countryCode.isNotEmpty()
        )
    }

    @Parcelize
    data class ViewState(
        val countryStatePairs: Map<AddressType, CountryStatePair> = mapOf(
            AddressType.BILLING to CountryStatePair(
                countryLocation = Location.EMPTY,
                stateLocation = Location.EMPTY
            ),
            AddressType.SHIPPING to CountryStatePair(
                countryLocation = Location.EMPTY,
                stateLocation = Location.EMPTY
            ),
        ),
        val isLoading: Boolean = false,
        val isStateSelectionEnabled: Boolean = false
    ) : Parcelable

    enum class AddressType {
        SHIPPING, BILLING
    }

    @Parcelize
    data class CountryStatePair(val countryLocation: Location, val stateLocation: Location) : Parcelable
}
