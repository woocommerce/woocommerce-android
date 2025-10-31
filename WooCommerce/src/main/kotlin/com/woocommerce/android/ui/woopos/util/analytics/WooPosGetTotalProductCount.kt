package com.woocommerce.android.ui.woopos.util.analytics

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WooPosGetTotalProductCount @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite,
    private val currentTime: CurrentTimeProvider,
    private val dispatchers: CoroutineDispatchers
) {
    private val mutex = Mutex()

    @Volatile private var totalProductCount: Int? = null
    @Volatile private var cacheTimestamp: Long = 0

    suspend operator fun invoke(): Int? = withContext(dispatchers.io) {
        mutex.withLock {
            return@withContext if (isCacheValid()) {
                totalProductCount
            } else {
                fetchProductCount()
            }
        }
    }

    private fun isCacheValid(): Boolean {
        if (totalProductCount == null) return false
        val elapsedTime = currentTime() - cacheTimestamp
        return elapsedTime < CACHE_DURATION_ONE_HOUR_MS
    }

    private suspend fun fetchProductCount(): Int? {
        val site = selectedSite.get()
        val result = fetchTotalProductCount(site)
        return if (result.isSuccess) {
            totalProductCount = result.getOrNull()
            cacheTimestamp = currentTime()
            totalProductCount
        } else {
            null
        }
    }

    private suspend fun fetchTotalProductCount(site: SiteModel): Result<Int?> {
        val response = productStore.fetchProductsCount(site)

        return if (response.isError) {
            Result.failure(Exception(response.error.message))
        } else {
            val count = response.model?.toInt()
            Result.success(count)
        }
    }

    companion object {
        private const val CACHE_DURATION_ONE_HOUR_MS = 60 * 60 * 1000L
    }

    class CurrentTimeProvider @Inject constructor() {
        operator fun invoke(): Long = System.currentTimeMillis()
    }
}
