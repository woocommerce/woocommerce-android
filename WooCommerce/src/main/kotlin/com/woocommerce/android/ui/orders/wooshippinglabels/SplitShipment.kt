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

    suspend operator fun invoke(orderId: Long, shipments: List<ShipmentUIModel>): Result<List<ShipmentUIModel>> {
        return selectedSite.getOrNull()?.let {
            val shipmentMap = shipments.toShipmentMap()

            val response = wooShippingLabelRepository.updateShipments(
                site = it,
                orderId = orderId,
                shipments = shipmentMap,
                shipmentIdsToUpdate = getShipmentsToUpdate(shipments)
            )
            val result = response.model
            if (response.isError || result == null) {
                Result.failure(Exception("Split shipment failed"))
            } else {
                updateCachedShipments(orderId, result.data)
                // Update remote ids
                val newShipments = shipments.map { it.copy(remoteId = it.id) }
                Result.success(newShipments)
            }
        } ?: Result.failure(Exception("No site selected"))
    }

    private fun getShipmentsToUpdate(shipments: List<ShipmentUIModel>): Map<String, Int> = shipments.filter {
        it.remoteId != null && it.id != it.remoteId
    }.associate { it.remoteId!! to it.id.toInt() }

    private fun List<ShipmentUIModel>.toShipmentMap() = associate {
        it.id to it.items.map { item -> Item(id = item.itemId, subItems = item.subItems()) }
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
