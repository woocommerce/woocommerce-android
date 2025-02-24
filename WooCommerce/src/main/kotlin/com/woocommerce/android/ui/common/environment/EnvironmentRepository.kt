package com.woocommerce.android.ui.common.environment

import androidx.annotation.VisibleForTesting
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.WooException
import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.network.environment.EnvironmentRestClient
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import javax.inject.Inject
import kotlin.math.pow

/**
 * Fetches, stores and delivers data from the Environment Rest Client.
 */
class EnvironmentRepository @Inject constructor(
    private val selectedSite: SelectedSite,
    private val environmentRestClient: EnvironmentRestClient,
    private val appPrefsWrapper: AppPrefsWrapper,
    private val dispatchers: CoroutineDispatchers
) {
    /**
     * Gets the `storeID`.
     * Can be retrieved locally or fetched from the remote source.
     */
    suspend fun fetchOrGetStoreID(site: SiteModel = selectedSite.get()): Result<String> {
        // If exists locally return it.
        val storedStoreID = appPrefsWrapper.getWCStoreID(site.siteId)
        if (storedStoreID.isNotNullOrEmpty()) {
            return Result.success(storedStoreID)
        }

        // If it doesn't exists, fetch it and store it locally.
        return withContext(dispatchers.io) {
            var retryCount = 0
            var result: Result<String>

            do {
                result = fetchStoreID(site)
                if (result.isFailure) {
                    retryCount++
                    delay(BACKOFF_RETRY_DELAY * BACKOFF_RETRY_EXPONENTIAL_FACTOR.pow(retryCount).toLong())
                }
            } while (result.isFailure && retryCount < MAX_RETRIES)

            return@withContext result
        }
    }

    private suspend fun fetchStoreID(site: SiteModel): Result<String> {
        val environmentResponse = environmentRestClient.fetchStoreEnvironment(site)
        return when {
            environmentResponse.isError -> {
                Result.failure(WooException(environmentResponse.error))
            }
            environmentResponse.result?.storeID != null -> {
                val storeID = environmentResponse.result!!.storeID!!
                appPrefsWrapper.setWCStoreID(site.siteId, storeID)
                Result.success(storeID)
            }
            else -> Result.failure(
                WooException(WooError(WooErrorType.GENERIC_ERROR, BaseRequest.GenericErrorType.UNKNOWN))
            )
        }
    }

    companion object {
        @VisibleForTesting
        const val MAX_RETRIES = 3

        @VisibleForTesting
        const val BACKOFF_RETRY_DELAY = 1000L

        @VisibleForTesting
        const val BACKOFF_RETRY_EXPONENTIAL_FACTOR = 2.0
    }
}
