package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

class WooPosRetrieveOrderRefunds @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(order: Order, forceRefresh: Boolean = false): Result<List<Refund>> =
        withContext(Dispatchers.IO) {
            val site = selectedSite.get()

            val refundModels = if (forceRefresh) {
                val fetchResult = refundStore.fetchAllRefunds(site, order.id)
                if (fetchResult.isError) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch refunds: ${fetchResult.error.message}")
                    )
                }
                fetchResult.model ?: emptyList()
            } else {
                val cachedRefunds = refundStore.getAllRefunds(site, order.id)
                cachedRefunds.ifEmpty {
                    val fetchResult = refundStore.fetchAllRefunds(site, order.id)
                    if (fetchResult.isError) {
                        return@withContext Result.failure(
                            Exception("Failed to fetch refunds: ${fetchResult.error.message}")
                        )
                    }
                    fetchResult.model ?: emptyList()
                }
            }

            Result.success(refundModels.map { it.toAppModel() })
        }
}
