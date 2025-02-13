package org.wordpress.android.fluxc.network.rest.wpcom.wc.admin

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooAdminStore @Inject constructor(
    private val restClient: WooAdminRestClient
) {
    suspend fun getOptions(
        site: SiteModel,
        keys: List<String>
    ): WooResult<Map<String, Any>> =
        restClient.getOptions(site, keys).asWooResult()

    suspend fun updateOptions(
        site: SiteModel,
        options: Map<String, Any>
    ): WooResult<Unit> = restClient.updateOptions(site, options).asWooResult()
}
