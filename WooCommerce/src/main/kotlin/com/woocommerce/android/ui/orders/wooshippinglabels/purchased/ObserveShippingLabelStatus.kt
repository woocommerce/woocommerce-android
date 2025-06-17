package com.woocommerce.android.ui.orders.wooshippinglabels.purchased

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PURCHASED
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.PURCHASE_IN_PROGRESS
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippingLabelStatus.UNKNOWN
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShippingLabelDTO
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveShippingLabelStatus @Inject constructor(
    private val selectedSite: SelectedSite,
    private val labelRepository: WooShippingLabelRepository,
    private val shippingConfigDataStore: WooShippingConfigDataStore
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
                val newsStatus = response?.status ?: UNKNOWN
                if (latestStatus != newsStatus) {
                    updateConfig(orderId, response)
                    latestStatus = newsStatus
                }
                emit(ObserveShippingLabelStatusResult(latestStatus, response))
                delay(DELAY_BETWEEN_STATUS_CHECKS)
            } while (latestStatus == PURCHASE_IN_PROGRESS)
        }
    }

    // Updates the cached config with the latest purchased label response.
    private suspend fun updateConfig(orderId: Long, response: ShippingLabelModel?) {
        if (response?.status != PURCHASED) return

        fun addPurchasedLabelToConfigDTO(
            currentOrderLabels: List<ShippingLabelDTO>,
            newLabel: ShippingLabelModel
        ): List<ShippingLabelDTO> {
            val alreadyHasSameLabel = currentOrderLabels.any { it.labelId == newLabel.labelId }
            return if (alreadyHasSameLabel) {
                currentOrderLabels.map {
                    if (it.labelId == newLabel.labelId) {
                        it.copy(
                            created = newLabel.created?.time,
                            status = newLabel.status,
                            refundableAmount = newLabel.refundableAmount
                        )
                    } else {
                        it
                    }
                }
            } else {
                // We map the new label to ShippingLabelDTO to save it into the Config Data Store. Not all properties
                // are mapped, as only a subset is needed for the UI. The config will be updated again later when the
                // config endpoint is called.
                currentOrderLabels + ShippingLabelDTO(
                    labelId = newLabel.labelId,
                    tracking = newLabel.tracking,
                    refundableAmount = newLabel.refundableAmount,
                    status = newLabel.status,
                    created = newLabel.created?.time,
                    carrierId = newLabel.carrierId,
                    currency = newLabel.currency,
                    expiryDate = newLabel.expiryDate,
                )
            }
        }

        val currentConfig = shippingConfigDataStore.observeConfig(orderId).first() ?: return
        val updatedConfig = currentConfig.copy(
            shippingLabelData = currentConfig.shippingLabelData.copy(
                currentOrderLabels = addPurchasedLabelToConfigDTO(
                    currentConfig.shippingLabelData.currentOrderLabels,
                    response
                )
            )
        )
        shippingConfigDataStore.saveConfig(orderId, updatedConfig)
    }

    data class ObserveShippingLabelStatusResult(
        val status: ShippingLabelStatus,
        val shippingLabelModel: ShippingLabelModel? = null
    )

    companion object {
        private const val DELAY_BETWEEN_STATUS_CHECKS = 2000L
    }
}
