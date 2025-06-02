package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.wooshippinglabels.datasource.WooShippingConfigDataStore
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.Item
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.ShipmentMap
import com.woocommerce.android.ui.orders.wooshippinglabels.networking.WooShippingLabelRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class SplitShipment @Inject constructor(
    private val selectedSite: SelectedSite,
    private val wooShippingLabelRepository: WooShippingLabelRepository,
    private val configDataStore: WooShippingConfigDataStore,
) {

    suspend operator fun invoke(orderId: Long, shipments: List<ShipmentUIModel>): Result<Unit> {
        return selectedSite.getOrNull()?.let {
            val shipmentMap = shipments.toShipmentMap() ?: return Result.failure(Exception("Shipment with null id"))

            val response = wooShippingLabelRepository.updateShipments(
                site = it,
                orderId = orderId,
                shipments = shipmentMap,
            )
            val result = response.model
            if (response.isError || result == null) {
                Result.failure(Exception("Split shipment failed"))
            } else {
                updateCachedShipments(orderId, result.data)
                Result.success(Unit)
            }
        } ?: Result.failure(Exception("No site selected"))
    }

    private fun List<ShipmentUIModel>.toShipmentMap(): ShipmentMap? {
        if (any { it.id == null }) {
            return null
        }
        return associate { it.id!! to it.items.map { item -> Item(id = item.itemId, subItems = item.subItems()) } }
    }

    /**
     * The endpoint expects subItems in the format:
     * if quantity is greater than 1, list of subItems e.g: ["6230-sub-0", "6230-sub-1"]
     * if quantity is 1, an empty list
     */
    private fun ShippableItemModel.subItems() = if (quantity > 1) {
        List(quantity.toInt()) { index -> "$itemId-sub-$index" }
    } else {
        emptyList()
    }

    private suspend fun updateCachedShipments(orderId: Long, newShipments: ShipmentMap) {
        configDataStore.observeConfig(orderId).first()?.let {
            configDataStore.saveConfig(orderId, it.copy(shipments = newShipments))
        }
    }
}
