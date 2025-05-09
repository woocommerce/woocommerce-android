package com.woocommerce.android.ui.orders.wooshippinglabels.split

import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShipmentUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import javax.inject.Inject

class GetSplitMovements @Inject constructor() {
    operator fun invoke(
        sourceShipmentKey: Int,
        shipments: Map<Int, ShipmentUIModel>,
        selection: Map<Int, SelectableShippableItemsUI>
    ): List<SplitMovement> {
        val sourceShipmentItems = mutableListOf<ShippableItemModel>()
        val nextShipmentItems = mutableListOf<ShippableItemModel>()

        selection[sourceShipmentKey]?.shippableItems?.forEachIndexed { index, item ->
            when {
                item is SelectableShippableItemUI.SingleSelectableShippableItemUI && item.isSelected -> {
                    nextShipmentItems.add(shipments.getValue(sourceShipmentKey).items[index])
                }

                item is SelectableShippableItemUI.SingleSelectableShippableItemUI && !item.isSelected -> {
                    sourceShipmentItems.add(shipments.getValue(sourceShipmentKey).items[index])
                }

                item is SelectableShippableItemUI.ExpandableSelectableShippableItemUI && item.isSelected -> {
                    nextShipmentItems.add(shipments.getValue(sourceShipmentKey).items[index])
                }

                item is SelectableShippableItemUI.ExpandableSelectableShippableItemUI &&
                    !item.isSelected &&
                    item.selectedIndexes.isNotEmpty() -> {
                    val selected = item.selectedIndexes.size
                    val sourceItem = shipments.getValue(sourceShipmentKey).items[index]

                    sourceShipmentItems.add(sourceItem.copy(quantity = sourceItem.quantity - selected))
                    nextShipmentItems.add(sourceItem.copy(quantity = selected.toFloat()))
                }

                else -> {
                    sourceShipmentItems.add(shipments.getValue(sourceShipmentKey).items[index])
                }
            }
        }

        return if (nextShipmentItems.isNotEmpty()) {
            getPossibleKeys(
                sourceShipmentKey = sourceShipmentKey,
                items = shipments,
                isRemoveMovement = sourceShipmentItems.isEmpty()
            ).map { key ->
                SplitMovement(
                    sourceShipmentKey = sourceShipmentKey,
                    updatedSourceShipmentItems = sourceShipmentItems,
                    destinationShipmentKey = key,
                    movingShipmentItems = nextShipmentItems
                )
            }
        } else {
            emptyList()
        }
    }

    private fun getPossibleKeys(
        sourceShipmentKey: Int,
        items: Map<Int, ShipmentUIModel>,
        isRemoveMovement: Boolean
    ): List<Int> {
        val otherKeys = items.keys.filter { it != sourceShipmentKey }
        if (isRemoveMovement) return otherKeys

        var nextKey = (otherKeys.maxOrNull() ?: sourceShipmentKey) + 1
        if (nextKey == sourceShipmentKey) nextKey++
        return otherKeys + nextKey
    }
}

data class SplitMovement(
    val sourceShipmentKey: Int,
    val updatedSourceShipmentItems: List<ShippableItemModel>,
    val destinationShipmentKey: Int,
    val movingShipmentItems: List<ShippableItemModel>
) {
    val totalItemsToMove: Int
        get() = movingShipmentItems.sumByFloat { it.quantity }.toInt()
    val isRemoveMovement: Boolean
        get() = updatedSourceShipmentItems.isEmpty()
}
