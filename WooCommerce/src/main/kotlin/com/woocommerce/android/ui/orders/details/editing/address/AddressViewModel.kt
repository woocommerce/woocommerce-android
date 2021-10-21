package com.woocommerce.android.ui.orders.details.editing.address

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.EditShippingLabelAddressViewModel
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

    init {
        loadCountriesAndStates()
    }

    fun loadCountriesAndStates() {
        launch {
            // we only fetch the countries and states if they've never been fetched
            if (countries.isEmpty()) {
                // TODO trigger loading dialog
                dataStore.fetchCountriesAndStates(selectedSite.get())
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
        val state: String = ""
    ) : Parcelable
}
