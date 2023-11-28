package com.woocommerce.android.ui.common.environment

import com.woocommerce.android.network.environment.EnvironmentRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

/**
 * Fetches, stores and delivers data from the Environment Rest Client.
 */
class EnvironmentRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val environmentRestClient: EnvironmentRestClient,
    private val dispatchers: CoroutineDispatchers
){
    /**
     * Gets the `storeID`.
     * Can be retrieved locally or fetched from the remote source.
     */
    suspend fun fetchOrGetStoreID(site: SiteModel = selectedSite.get()): WooResult<String?> {
        // TODO: Store `storeID` locally
        // TODO: Get `storeID` if locally available
        return withContext(dispatchers.io) {
            val environmentResponse = environmentRestClient.fetchStoreEnvironment(site)
            when {
                environmentResponse.isError -> {
                    WooResult(environmentResponse.error)
                }
                environmentResponse.result != null -> {
                    val storeID = environmentResponse.result.storeID
                    WooResult(storeID)
                }
                else -> WooResult(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            }
        }
    }
}
