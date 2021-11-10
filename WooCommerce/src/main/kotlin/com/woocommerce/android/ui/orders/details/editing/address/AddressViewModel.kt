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

    val states: List<Location>
        get() = dataStore.getStates(viewState.countryLocation.code).map { it.toAppModel() }

    val countryLocation: Location
        get() = viewState.countryLocation

    val stateLocation: Location
        get() = viewState.stateLocation

    private var hasStarted = false

    /**
     * The start method is called when the view is created. When the view is recreated (e.g. navigating to country
     * search and back) we don't want this method to be called again, otherwise the ViewModel will replace the newly
     * selected country or state with the previously saved values.
     *
     * @see applyCountryStateChangesSafely
     */
    fun start(countryCode: String, stateCode: String) {
        if (hasStarted) {
            return
        }
        hasStarted = true
        loadCountriesAndStates(countryCode, stateCode)
        viewState.applyCountryStateChangesSafely(countryCode, stateCode)
    }

    private fun loadCountriesAndStates(countryCode: String, stateCode: String) {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState.copy(
                    isLoading = false
                ).applyCountryStateChangesSafely(countryCode, stateCode)
            }
        }
    }

    fun hasCountries() = countries.isNotEmpty()

    fun hasStates() = states.isNotEmpty()

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

    private fun getStateNameFromCode(stateCode: String): String {
        return states.find { it.code == stateCode }?.name ?: stateCode
    }

    private fun getStateLocationFromCode(stateCode: String): Location {
        return Location(stateCode, getStateNameFromCode(stateCode))
    }

    fun onCountrySelected(countryCode: String) {
        if (countryCode != viewState.countryLocation.code) {
            viewState = viewState.copy(
                countryLocation = getCountryLocationFromCode(countryCode),
                stateLocation = Location("", ""),
                isStateSelectionEnabled = true
            )
        }
    }

    fun onStateSelected(stateCode: String) {
        if (stateCode != viewState.stateLocation.code) {
            viewState = viewState.copy(
                stateLocation = getStateLocationFromCode(stateCode)
            )
        }
    }

    /**
     * State data acquisition depends on the Country configuration, so when updating the ViewState
     * we need to make sure that we updated the Country code before applying everything else to avoid
     * looking into a outdated state information
     */
    private fun ViewState.applyCountryStateChangesSafely(countryCode: String, stateCode: String) {
        val countryLocation = getCountryLocationFromCode(countryCode)

        viewState = viewState.copy(
            countryLocation = countryLocation
        )
        viewState = this.copy(
            countryLocation = countryLocation,
            stateLocation = getStateLocationFromCode(stateCode),
            isStateSelectionEnabled = countryCode.isNotEmpty()
        )
    }

    @Parcelize
    data class ViewState(
        val countryLocation: Location = Location("", ""),
        val stateLocation: Location = Location("", ""),
        val isLoading: Boolean = false,
        val isStateSelectionEnabled: Boolean = false
    ) : Parcelable
}
