package com.woocommerce.android.model

import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class GetLocations @Inject constructor(private val locationStore: WCDataStore) {
    operator fun invoke(countryCode: LocationCode, stateCode: LocationCode): Pair<Location, AmbiguousLocation> {
        val country = locationStore.getCountries()
            .firstOrNull { it.code == countryCode }
            ?.toAppModel()
            ?: Location(code = countryCode, name = countryCode)

        val state = locationStore.getStates(countryCode)
            .firstOrNull { it.code == stateCode }
            ?.toAppModel()
            ?.let { AmbiguousLocation.Defined(it) }
            ?: AmbiguousLocation.Raw(stateCode)

        return country to state
    }
}
