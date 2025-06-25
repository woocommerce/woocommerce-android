package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
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
    operator fun invoke(orderId: Long, labelId: Long): Flow<ObserveShippingLabelStatusResult> {
        return flow {
            var latestStatus = PURCHASE_IN_PROGRESS
            emit(ObserveShippingLabelStatusResult(latestStatus))

            do {
                val response = labelRepository.fetchShippingLabelStatus(
                    site = selectedSite.get(),
                    orderId = orderId,
                    labelId = labelId
                ).takeIf { it.isError.not() }?.model
                latestStatus = response?.status ?: UNKNOWN
                emit(ObserveShippingLabelStatusResult(latestStatus, response))
                delay(DELAY_BETWEEN_STATUS_CHECKS)
            } while (latestStatus == PURCHASE_IN_PROGRESS)
        }
    }

    data class ObserveShippingLabelStatusResult(
        val status: ShippingLabelStatus,
        val shippingLabelModel: ShippingLabelModel? = null
    )

    companion object {
        private const val DELAY_BETWEEN_STATUS_CHECKS = 2000L
    }
}
