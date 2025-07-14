package com.woocommerce.android.ui.prefs.privacy

import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.geo.WpComGeoRestClient
import javax.inject.Inject

class GeoRepository @Inject constructor(
    private val wpComGeoRestClient: WpComGeoRestClient,
    private val dispatchers: CoroutineDispatchers
) {
    suspend fun fetchCountryCode(): Result<String> {
        return withContext(dispatchers.io) {
            wpComGeoRestClient.fetchCountryCode().map { it.orEmpty() }
        }
    }
}
