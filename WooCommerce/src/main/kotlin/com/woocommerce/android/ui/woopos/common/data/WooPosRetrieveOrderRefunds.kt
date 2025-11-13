package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import javax.inject.Inject

class WooPosRetrieveOrderRefunds @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(order: Order): Result<List<Refund>> =
        withContext(Dispatchers.IO) {
            if (order.refundTotal.compareTo(BigDecimal.ZERO) == 0) {
                return@withContext Result.success(emptyList())
            }

            val site = selectedSite.get()

            var refundModels = refundStore.getAllRefunds(site, order.id)
            if (refundModels.isEmpty()) {
                val fetchResult = refundStore.fetchAllRefunds(site, order.id)
                if (fetchResult.isError) {
                    return@withContext Result.failure(
                        Exception("Failed to fetch refunds: ${fetchResult.error.message}")
                    )
                }
                refundModels = fetchResult.model ?: emptyList()
            }

            Result.success(refundModels.map { it.toAppModel() })
        }
}
