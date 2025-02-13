package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JitmRestClient
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmStore @Inject constructor(
    private val coroutineEngine: CoroutineEngine,
    private val restClient: JitmRestClient
) {
    suspend fun fetchJitmMessage(
        site: SiteModel,
        messagePath: String,
        query: String,
    ): WooResult<Array<JITMApiResponse>> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "jitm") {
            restClient.fetchJitmMessage(site, messagePath, query).asWooResult()
        }
    }

    suspend fun dismissJitmMessage(
        site: SiteModel,
        jitmId: String,
        featureClass: String,
    ): WooResult<Boolean> {
        return coroutineEngine.withDefaultContext(AppLog.T.API, this, "jitm-dismiss") {
            restClient.dismissJitmMessage(site, jitmId, featureClass).asWooResult()
        }
    }
}
