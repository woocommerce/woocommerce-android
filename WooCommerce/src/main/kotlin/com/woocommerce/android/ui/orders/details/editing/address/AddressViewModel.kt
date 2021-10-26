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
        get() = dataStore.getStates(viewState.countryCode).map { it.toAppModel() }

    fun start(country: String, state: String) {
        viewState = viewState.copy(countryCode = country, stateCode = state)
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

    fun hasCountries() = countries.isNotEmpty()

    fun hasStates() = states.isNotEmpty()

    fun getCountryCodeFromCountryName(countryName: String): String {
        return countries.find { it.name == countryName }?.code ?: countryName
    }

    fun getCountryNameFromCountryCode(countryCode: String): String {
        return countries.find { it.code == countryCode }?.name ?: countryCode
    }

    fun onCountrySelected(countryCode: String) {
        if (countryCode != viewState.countryCode) {
            viewState = viewState.copy(countryCode = countryCode)
        }
    }

    fun onStateSelected(stateCode: String) {
        if (stateCode != viewState.stateCode) {
            viewState = viewState.copy(stateCode = stateCode)
        }
    }

    @Parcelize
    data class ViewState(
        val countryCode: String = "",
        val stateCode: String = "",
        val isLoading: Boolean = false
    ) : Parcelable
}
