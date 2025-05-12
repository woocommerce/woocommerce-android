package com.woocommerce.android.ui.woopos.products

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.WCProductStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTotalProductCount @Inject constructor(
    private val productStore: WCProductStore,
    private val selectedSite: SelectedSite
) {
    private val mutex = Mutex()
    private var totalProductCount: Int? = null

    suspend operator fun invoke(): Int? {
        if (totalProductCount == null) {
            fetchProductCount()
        }
        return totalProductCount
    }

    private suspend fun fetchProductCount() {
        mutex.withLock {
            if (totalProductCount != null) return
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
