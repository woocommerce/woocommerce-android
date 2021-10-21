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
        get() = dataStore.getStates(viewState.country).map { it.toAppModel() }

    fun start(country: String, state: String) {
        viewState = viewState.copy(country = country, state = state)
        loadCountriesAndStates()
    }

    fun loadCountriesAndStates() {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                viewState = viewState.copy(isLoading = true)
                dataStore.fetchCountriesAndStates(selectedSite.get())
                viewState = viewState.copy(isLoading = false)
            }
        }
    }

    fun onCountrySelected(countryCode: String) {
        if (countryCode != viewState.country) {
            viewState = viewState.copy(country = countryCode)
        }
    }

    fun onStateSelected(stateCode: String) {
        if (stateCode != viewState.country) {
            viewState = viewState.copy(state = stateCode)
        }
    }

    @Parcelize
    data class ViewState(
        val country: String = "",
        val state: String = "",
        val isLoading: Boolean = false
    ) : Parcelable
}
