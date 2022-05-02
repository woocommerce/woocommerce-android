package com.woocommerce.android.ui.cardreader.connect

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.cardreader.onboarding.PluginType
import com.woocommerce.android.ui.cardreader.onboarding.toInPersonPaymentsPluginType
import org.wordpress.android.fluxc.model.payments.inperson.WCTerminalStoreLocationErrorType
import org.wordpress.android.fluxc.store.WCInPersonPaymentsStore
import javax.inject.Inject

class CardReaderLocationRepository @Inject constructor(
    private val inPersonPaymentsStore: WCInPersonPaymentsStore,
    private val selectedSite: SelectedSite
) {
    suspend fun getDefaultLocationId(pluginType: PluginType): LocationIdFetchingResult {
        val selectedSite = selectedSite.getIfExists() ?: return LocationIdFetchingResult.Error.Other(
            "Missing selected site"
        )
        val result = inPersonPaymentsStore.getStoreLocationForSite(
            pluginType.toInPersonPaymentsPluginType(),
            selectedSite
        )
        return if (result.isError) {
            when (val type = result.error?.type) {
                is WCTerminalStoreLocationErrorType.MissingAddress -> {
                    LocationIdFetchingResult.Error.MissingAddress(type.addressEditingUrl)
                }
                is WCTerminalStoreLocationErrorType.InvalidPostalCode -> {
                    LocationIdFetchingResult.Error.InvalidPostalCode
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
            object InvalidPostalCode : Error()
            data class Other(val error: String?) : Error()
        }

        data class Success(val locationId: String) : LocationIdFetchingResult()
    }
}
