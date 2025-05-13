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
    private val dispatchers: CoroutineDispatchers
) {
    private val mutex = Mutex()
    @Volatile private var totalProductCount: Int? = null

    suspend operator fun invoke(): Int? = withContext(dispatchers.io) {
        totalProductCount?.let { return@withContext it }
        fetchProductCount()
        return@withContext totalProductCount
    }

    private suspend fun fetchProductCount() {
        mutex.withLock {
            val site = selectedSite.get()
            val result = fetchTotalProductCount(site)
            if (result.isSuccess) {
                totalProductCount = result.getOrNull()
            }
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
}
