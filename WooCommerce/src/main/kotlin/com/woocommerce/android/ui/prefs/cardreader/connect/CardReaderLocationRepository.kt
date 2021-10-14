package com.woocommerce.android.ui.prefs.cardreader.connect

import com.woocommerce.android.tools.SelectedSite
import org.wordpress.android.fluxc.model.pay.WCTerminalStoreLocationErrorType
import org.wordpress.android.fluxc.store.WCPayStore
import javax.inject.Inject

class CardReaderLocationRepository @Inject constructor(
    private val wcPayStore: WCPayStore,
    private val selectedSite: SelectedSite
) {
    suspend fun getDefaultLocationId(): LocationIdFetchingResult {
        val selectedSite = selectedSite.getIfExists() ?: return LocationIdFetchingResult.Error.Other(
            "Missing selected site"
        )
        val result = wcPayStore.getStoreLocationForSite(selectedSite)
        return if (result.isError) {
            when (val type = result.error?.type) {
                is WCTerminalStoreLocationErrorType.MissingAddress -> {
                    LocationIdFetchingResult.Error.MissingAddress(type.addressEditingUrl)
                }
                else -> LocationIdFetchingResult.Error.Other(result.error?.message)
            }
        } else {
            LocationIdFetchingResult.Success(result.locationId!!)
        }
    }

    sealed class LocationIdFetchingResult {
        sealed class Error : LocationIdFetchingResult() {
            data class MissingAddress(val url: String) : Error()
            data class Other(val error: String?) : Error()
        }

        data class Success(val locationId: String) : LocationIdFetchingResult()
    }
}
