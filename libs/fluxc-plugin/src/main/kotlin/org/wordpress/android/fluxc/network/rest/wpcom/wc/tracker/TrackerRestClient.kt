package org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker

import org.wordpress.android.fluxc.generated.endpoint.WOOCOMMERCE
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooNetwork
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.utils.putIfNotEmpty
import org.wordpress.android.fluxc.utils.toWooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TrackerRestClient @Inject constructor(private val wooNetwork: WooNetwork) {
    suspend fun sendTelemetry(
        appVersion: String,
        site: SiteModel,
        installationDate: String?
    ): WooPayload<Unit> {
        val url = WOOCOMMERCE.tracker.pathWcTelemetry

        val response = wooNetwork.executePostGsonRequest(
            site = site,
            path = url,
            clazz = Unit::class.java,
            body = mutableMapOf(
                "platform" to "android",
                "version" to appVersion,
            ).putIfNotEmpty("installation_date" to installationDate)
        )

        return response.toWooPayload()
    }
}
