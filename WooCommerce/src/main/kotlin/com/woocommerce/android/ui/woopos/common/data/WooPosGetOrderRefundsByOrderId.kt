package com.woocommerce.android.ui.woopos.common.data

import com.woocommerce.android.model.Refund
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.store.WCRefundStore
import javax.inject.Inject

class WooPosGetOrderRefundsByOrderId @Inject constructor(
    private val refundStore: WCRefundStore,
    private val selectedSite: SelectedSite
) {
    suspend operator fun invoke(orderId: Long): List<Refund> {
        return withContext(Dispatchers.IO) {
            refundStore.getAllRefunds(selectedSite.get(), orderId)
                .map { it.toAppModel() }
        }
    }
}
