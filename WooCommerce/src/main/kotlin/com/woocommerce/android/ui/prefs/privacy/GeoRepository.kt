package com.woocommerce.android.ui.prefs.privacy

import com.woocommerce.android.WooException
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class GeoRepository @Inject constructor(
    private val wpComGeoRestClient: WpComGeoRestClient,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun fetchCountryCode(): Result<String> {

        return withContext(dispatchers.io) {
            val response = wpComGeoRestClient.fetchCountryCode()

            if (response.isError) {
                Result.failure(WooException(response.error))
            } else {
                Result.success(response.result.orEmpty())
            }
        }
    }
}
