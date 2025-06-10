package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PURCHASE_IN_PROGRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.UNKNOWN
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveShippingLabelStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val labelRepository: WooShippingLabelRepository
) {
    operator fun invoke(orderId: Long, labelId: Long): Flow<ShippingLabelStatus> {
        return flow {
            var latestStatus = PURCHASE_IN_PROGRESS
            emit(latestStatus)

            do {
                latestStatus = labelRepository.fetchShippingLabelStatus(
                    site = selectedSite.get(),
                    orderId = orderId,
                    labelId = labelId
                ).takeIf { it.isError.not() }?.model ?: UNKNOWN
                emit(latestStatus)
                delay(DELAY_BETWEEN_STATUS_CHECKS)
            } while (latestStatus == PURCHASE_IN_PROGRESS)
        }
    }

    companion object {
        private const val DELAY_BETWEEN_STATUS_CHECKS = 2000L
    }
}
