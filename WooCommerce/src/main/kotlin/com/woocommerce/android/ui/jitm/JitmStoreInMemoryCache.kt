package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class JitmStoreInMemoryCache
@Inject constructor(
    private val selectedSite: SelectedSite,
    private val pathsProvider: JitmMessagePathsProvider,
    private val jitmStore: JitmStoreWrapper,
    private val queryParamsEncoder: QueryParamsEncoder,
    private val jitmTracker: JitmTracker,
    private var appCoroutineScope: CoroutineScope
) {
    private val cache = ConcurrentHashMap<String, CopyOnWriteArrayList<JITMApiResponse>>()

    @Volatile
    private var cacheInitContinuation: Continuation<Unit>? = null

    @Volatile
    private var initialisationStatus = InitStatus.NOT_STARTED

    suspend fun init() {
        if (!selectedSite.exists() || initialisationStatus != InitStatus.NOT_STARTED) return

        initialisationStatus = InitStatus.STARTED

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

        cacheInitContinuation?.resume(Unit)
        initialisationStatus = InitStatus.DONE
    }

    suspend fun getMessagesForPath(messagePath: String): List<JITMApiResponse> {
        WooLog.d(WooLog.T.JITM, "Getting JITM messages for path: $messagePath")
        if (!selectedSite.exists()) return emptyList()

        when (initialisationStatus) {
            InitStatus.NOT_STARTED -> {
                appCoroutineScope.launch { init() }
                suspendCoroutine { cacheInitContinuation = it }
            }
            InitStatus.STARTED -> suspendCoroutine { cacheInitContinuation = it }
            InitStatus.DONE -> {
                // cache initialization is done, use it
            }
        }
        cache.putIfAbsent(messagePath, CopyOnWriteArrayList())
        return cache[messagePath]!!
    }

    suspend fun dismissJitmMessage(messagePath: String, jitmId: String, featureClass: String): WooResult<Boolean> {
        if (!selectedSite.exists()) WooResult(false)

        evictFirstMessage(messagePath)
        return jitmStore.dismissJitmMessage(selectedSite.get(), jitmId, featureClass)
    }

    private fun handleResponse(path: String, response: WooResult<Array<JITMApiResponse>>) {
        val utmSource = path.split(":")[1]
        if (!response.isError) {
            jitmTracker.trackJitmFetchSuccess(utmSource, response.model?.getOrNull(0)?.id, response.model?.size)
            WooLog.d(WooLog.T.JITM, "Successfully fetched ${response.model?.size} JITM messages for path: $path")
            response.model?.let {
                cache[path] = CopyOnWriteArrayList(it.toList())
            }
        } else {
            WooLog.e(WooLog.T.JITM, "Failed to fetch JITM message for path: $path, error: ${response.error}")
            jitmTracker.trackJitmFetchFailure(utmSource, response.error.type, response.error.message)
        }
    }

    private fun evictFirstMessage(messagePath: String) {
        cache[messagePath]?.let { if (it.isNotEmpty()) it.removeAt(0) }
    }

    private enum class InitStatus {
        NOT_STARTED, STARTED, DONE,
    }
}
