package com.woocommerce.android.model

import com.woocommerce.android.ui.orders.details.editing.address.LocationCode
import kotlinx.coroutines.runBlocking
import org.wordpress.android.fluxc.store.WCDataStore
import javax.inject.Inject

class GetLocations @Inject constructor(private val locationStore: WCDataStore) {
    suspend operator fun invoke(countryCode: LocationCode, stateCode: LocationCode): Pair<Location, AmbiguousLocation> {
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

    /**
     * Only for the legacy shipping labels flow, which resolves locations outside of a coroutine.
     * Delete along with that flow.
     */
    fun getBlocking(countryCode: LocationCode, stateCode: LocationCode): Pair<Location, AmbiguousLocation> =
        runBlocking { invoke(countryCode, stateCode) }
}
