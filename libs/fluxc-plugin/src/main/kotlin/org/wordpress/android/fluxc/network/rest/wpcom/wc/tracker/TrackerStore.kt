package org.wordpress.android.fluxc.network.rest.wpcom.wc.tracker

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TrackerStore @Inject internal constructor(private val restClient: TrackerRestClient) {
    suspend fun sendTelemetry(
        appVersion: String,
        site: SiteModel,
        installationDateIso8601: String?
    ): WooPayload<Unit> {
        return restClient.sendTelemetry(appVersion, site, installationDateIso8601)
    }
}
