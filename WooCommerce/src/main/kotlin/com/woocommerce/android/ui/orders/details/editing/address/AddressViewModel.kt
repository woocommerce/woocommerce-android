package com.woocommerce.android.ui.orders.details.editing.address

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

@HiltViewModel
class AddressViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val selectedSite: SelectedSite,
    private val dataStore: WCDataStore,
) : ScopedViewModel(savedState) {
    val countries: List<Location>
        get() {
            return dataStore.getCountries().map { it.toAppModel() }
        }

    val states: List<Location>
        get() {
            return dataStore.getStates(country).map { it.toAppModel() }
        }

    var country: String = ""
        get() = field
        set(value) {
            if (value != field) {
                field = value
            }
        }

    init {
        loadCountriesAndStates()
    }

    fun loadCountriesAndStates() {
        launch {
            if (countries.isEmpty()) {
                dataStore.fetchCountriesAndStates(selectedSite.get())
            }
        }
    }
}
