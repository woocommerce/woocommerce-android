package com.woocommerce.android.ui.woopos.products

import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.product.ProductRestClient
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GetTotalProductCount @Inject constructor(
    private val productRestClient: ProductRestClient,
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

    private suspend fun fetchTotalProductCount(site: SiteModel): Result<Int> {
        val response = productRestClient.fetchProductsTotals(site)

        return if (response.isError) {
            Result.failure(Exception(response.error.message.orEmpty()))
        } else {
            val count = response.result?.sumOf { it.total.toInt() } ?: 0
            Result.success(count)
        }
    }
}
