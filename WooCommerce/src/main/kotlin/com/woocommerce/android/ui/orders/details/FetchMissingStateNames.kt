package com.woocommerce.android.ui.orders.details

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.addressformatting.displaysStateName
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

/**
 * Fetches the countries and states list when one of the addresses shows a state name (see [displaysStateName])
 * and its country isn't stored yet. Returns true when the list was fetched, so the caller can re-map the addresses.
 */
class FetchMissingStateNames @Inject constructor(
    private val dataStore: WCDataStore,
    private val selectedSite: SelectedSite,
) {
    suspend operator fun invoke(addresses: List<Address>): Boolean {
        val countriesMissingStateNames = addresses
            .filter { it.state is AmbiguousLocation.Raw && it.state.isNotEmpty() && displaysStateName(it.country.code) }
            .map { it.country.code }
            .distinct()
        if (countriesMissingStateNames.none { dataStore.getCountry(it) == null }) return false

        val site = selectedSite.getOrNull() ?: return false
        return !dataStore.fetchCountriesAndStates(site).isError
    }
}
