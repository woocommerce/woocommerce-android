package com.woocommerce.android.ui.jitm

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooLog
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import org.wordpress.android.fluxc.network.rest.wpcom.wc.jitm.JITMApiResponse
import org.wordpress.android.fluxc.store.JitmStore
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JitmStoreInMemoryCache
@Inject constructor(
    private val pathsProvider: JitmMessagePathsProvider,
    private val realStore: JitmStore,
    private val queryParamsEncoder: QueryParamsEncoder,
    private val selectedSite: SelectedSite,
) {
    private val cache = ConcurrentHashMap<String, Array<JITMApiResponse>>()

    suspend fun initCache() {
        supervisorScope {
            pathsProvider.paths.map { path ->
                async {
                    WooLog.d(WooLog.T.JITM, "Fetching JITM message for path: $path")
                    val jitmResult = realStore.fetchJitmMessage(
                        selectedSite.get(),
                        path,
                        queryParamsEncoder.getEncodedQueryParams(),
                    )
                    if (!jitmResult.isError) {
                        WooLog.d(
                            WooLog.T.JITM,
                            "Successfully fetched ${jitmResult.model?.size} JITM messages for path: $path"
                        )
                        jitmResult.model?.let { cache[path] = it }
                    } else {
                        WooLog.e(
                            WooLog.T.JITM,
                            "Failed to fetch JITM message for path: $path, error: ${jitmResult.error}"
                        )
                    }
                }
            }.awaitAll()
        }
    }

}
