package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmStoreInMemoryCache
@Inject constructor(
    private val selectedSite: SelectedSite,
    private val pathsProvider: JitmMessagePathsProvider,
    private val jitmStore: JitmStore,
    private val queryParamsEncoder: QueryParamsEncoder,
    private val jitmTracker: JitmTracker,
) {
    private val cache = ConcurrentHashMap<String, Array<JITMApiResponse>>()

    suspend fun init() {
        if (!selectedSite.exists()) return

        supervisorScope {
            pathsProvider.paths.map { path ->
                async {
                    WooLog.d(WooLog.T.JITM, "Fetching JITM message for path: $path")
                    val response = jitmStore.fetchJitmMessage(
                        selectedSite.get(),
                        path,
                        queryParamsEncoder.getEncodedQueryParams(),
                    )
                    handleResponse(path, response)
                }
            }.awaitAll()
        }
    }

    fun getMessage(messagePath: String): JITMApiResponse? {
        val cachedResponse = cache[messagePath]
        val message = cachedResponse?.firstOrNull()
        if (message == null) {
            WooLog.e(WooLog.T.JITM, "Failed to get JITM message for path: $messagePath from cache")
        }
        return message
    }

    suspend fun dismissJitmMessage(messagePath: String, jitmId: String, featureClass: String): WooResult<Boolean> {
        if (!selectedSite.exists()) WooResult(false)

        evictFirstMessage(messagePath)
        return jitmStore.dismissJitmMessage(selectedSite.get(), jitmId, featureClass)
    }

    private fun handleResponse(path: String, response: WooResult<Array<JITMApiResponse>>) {
        val utmSource = path.split(":")[1]
        if (!response.isError) {
            jitmTracker.trackJitmFetchSuccess(
                utmSource,
                response.model?.getOrNull(0)?.id,
                response.model?.size
            )
            WooLog.d(WooLog.T.JITM, "Successfully fetched ${response.model?.size} JITM messages for path: $path")
            response.model?.let { cache[path] = it }
        } else {
            jitmTracker.trackJitmFetchFailure(utmSource, response.error.type, response.error.message)
            WooLog.e(WooLog.T.JITM, "Failed to fetch JITM message for path: $path, error: ${response.error}")
        }
    }

    private fun evictFirstMessage(messagePath: String) {
        val jitmApiResponses = cache[messagePath]
        if (jitmApiResponses.isNullOrEmpty()) return
        cache[messagePath] = jitmApiResponses.copyOfRange(1, jitmApiResponses.size)
    }
}
