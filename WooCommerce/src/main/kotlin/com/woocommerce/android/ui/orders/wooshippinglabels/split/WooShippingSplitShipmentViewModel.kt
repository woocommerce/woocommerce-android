package com.woocommerce.android.ui.orders.wooshippinglabels.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.R
import com.woocommerce.android.extensions.sumByFloat
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemUI
import com.woocommerce.android.ui.orders.wooshippinglabels.components.ShippingLabelsSnackbarData
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.ui.orders.wooshippinglabels.toSelectableUIModel
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WooShippingSplitShipmentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val getSplitMovements: GetSplitMovements
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingSplitShipmentFragmentArgs by savedState.navArgs()
    private val storeOptions = navArgs.shipmentArgs.storeOptions

    private val currentShipments = MutableStateFlow(navArgs.shipmentArgs.shipments)
    private val shipmentsUIMap: MutableStateFlow<Map<Int, SelectableShippableItemsUI>?> = MutableStateFlow(null)

    private val shipmentSelected = MutableStateFlow(navArgs.shipmentArgs.shipments.keys.first())
    private val removeShipmentSheet: MutableStateFlow<RemoveShipmentSheet?> = MutableStateFlow(null)
    private val splitMessage: MutableStateFlow<SplitShipmentMessage?> = MutableStateFlow(null)

    init {
        launch {
            delay(TOOLTIP_DELAY)
            splitMessage.value = SplitShipmentMessage.Instructions
        }
        launch {
            currentShipments.collectLatest { shipments ->
                shipmentsUIMap.value = shipments.mapValues {
                    it.value.toSelectableUIModel(
                        currencyFormatter = currencyFormatter,
                        dimensionUnit = storeOptions.dimensionUnit,
                        weightUnit = storeOptions.weightUnit
                    )
                }
            }
        }
    }

    val viewState = combine(
        shipmentSelected,
        shipmentsUIMap.filterNotNull(),
        removeShipmentSheet,
        splitMessage
    ) { shipmentSelected, selectableItems, sheet, message ->
        SplitShipmentViewState(
            shipmentSelected = shipmentSelected,
            selectableItems = selectableItems,
            splitMovements = getSplitMovements(
                sourceShipmentKey = shipmentSelected,
                shipments = currentShipments.value,
                selection = selectableItems
            ),
            removeShipmentSheet = sheet,
            splitMessage = message
        )
    }.asLiveData()

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onDismissInstructions() {
        splitMessage.value = null
    }

    fun onUpdateSelectedShipment(shipmentKey: Int) {
        shipmentSelected.update { shipmentKey }
    }

    fun onRemoveShipmentMenuTapped(shipmentKeys: List<Int>) {
        val shipmentsMap = shipmentsUIMap.value ?: return
        removeShipmentSheet.value = RemoveShipmentSheet(
            removingShipments = shipmentsMap.filter { it.key in shipmentKeys },
            otherShipments = shipmentsMap.filter { it.key !in shipmentKeys },
        )
    }

    fun onRemoveShipments(removingShipmentKeys: List<Int>, destinationShipmentKey: Int?) {
        if (destinationShipmentKey != null && removingShipmentKeys.size == 1) {
            val removingShipmentKey = removingShipmentKeys.first()
            val movingShipmentItems = currentShipments.value[removingShipmentKey] ?: return
            onUpdateShipment(
                SplitMovement(
                    sourceShipmentKey = removingShipmentKey,
                    updatedSourceShipmentItems = emptyList(),
                    destinationShipmentKey = destinationShipmentKey,
                    movingShipmentItems = movingShipmentItems
                )
            )
        } else {
            // Merging all unfulfilled shipments
        }
        removeShipmentSheet.value = null
    }

    fun onDismissRemoveSheet() {
        removeShipmentSheet.value = null
    }

    fun onUpdateSelection(shippableItemIndex: Int, selectedIndexes: Set<Int>? = null) {
        val shipmentsMap = shipmentsUIMap.value?.toMutableMap() ?: return
        val items = shipmentsMap.getValue(shipmentSelected.value)
        val item = items.shippableItems[shippableItemIndex]
        val updatedItem = when (item) {
            is SelectableShippableItemUI.SingleSelectableShippableItemUI -> {
                item.copy(isSelected = !item.isSelected)
            }

            is SelectableShippableItemUI.ExpandableSelectableShippableItemUI -> {
                val indexes = when {
                    selectedIndexes == null && item.isSelected -> emptySet<Int>()
                    selectedIndexes == null -> List(item.shippableItem.quantity.toInt()) { it }.toSet()
                    else -> selectedIndexes
                }
                item.copy(selectedIndexes = indexes)
            }
        }
        val updatedList = items.shippableItems.toMutableList()
        updatedList[shippableItemIndex] = updatedItem
        shipmentsMap[shipmentSelected.value] = items.copy(shippableItems = updatedList)
        shipmentsUIMap.value = shipmentsMap
    }

    fun onUpdateShipment(splitMovement: SplitMovement) {
        val currentShipmentBackup = currentShipments.value
        currentShipments.update {
            val shipments = it.toMutableMap()
            if (splitMovement.updatedSourceShipmentItems.isEmpty()) {
                shipments.remove(splitMovement.sourceShipmentKey)
            } else {
                shipments[splitMovement.sourceShipmentKey] = splitMovement.updatedSourceShipmentItems
            }
            shipments[splitMovement.destinationShipmentKey] = shipments[splitMovement.destinationShipmentKey]
                ?.takeIf { it.isNotEmpty() }
                ?.combine(splitMovement.movingShipmentItems)
                ?: splitMovement.movingShipmentItems
            reindexShipments(shipments)
        }

        if (currentShipments.value.size == 1) {
            // The shipments row was removed. Since the pager can no longer manage the selected shipment, update it
            // manually.
            shipmentSelected.update { currentShipments.value.keys.first() }
        }

        val undoAction = {
            currentShipments.update {
                if (!currentShipmentBackup.keys.contains(shipmentSelected.value)) {
                    onUpdateSelectedShipment(currentShipmentBackup.keys.first())
                }
                currentShipmentBackup
            }
        }

        showUndoSnackbar(splitMovement, undoAction)
    }

    /**
     * Reindexes the keys of the given shipments so that they form a consecutive sequence starting from 0. For example,
     * if "Shipment 1" was removed from "Shipment 0, Shipment 1, Shipment 2", the remaining shipments will be reindexed
     * as "Shipment 0, Shipment 1".
     */
    private fun reindexShipments(
        shipments: Map<Int, List<ShippableItemModel>>
    ) = shipments.values.mapIndexed { index, items -> index to items }.toMap()

    private fun showUndoSnackbar(splitMovement: SplitMovement, undoAction: () -> Unit) {
        val snackbarMessage = if (splitMovement.totalItemsToMove > 1) {
            R.string.woo_shipping_split_shipment_moved_notice_plural
        } else {
            R.string.woo_shipping_split_shipment_moved_notice_one
        }

        splitMessage.value = SplitShipmentMessage.Success(
            ShippingLabelsSnackbarData(
                message = snackbarMessage,
                messageParameters = listOf(splitMovement.totalItemsToMove, splitMovement.destinationShipmentKey + 1),
                actionLabel = R.string.undo,
                dismissAction = { splitMessage.value = null },
                action = undoAction
            )
        )
    }

    data class SplitShipmentViewState(
        val shipmentSelected: Int,
        val selectableItems: Map<Int, SelectableShippableItemsUI>,
        val splitMovements: List<SplitMovement> = emptyList(),
        val removeShipmentSheet: RemoveShipmentSheet? = null,
        val splitMessage: SplitShipmentMessage? = null
    )

    companion object {
        private const val TOOLTIP_DELAY = 800L
    }
}

data class SelectableShippableItemsUI(
    val shippableItems: List<SelectableShippableItemUI>,
    val formattedTotalWeight: String,
    val formattedTotalPrice: String
) {
    val totalItemQuantity: Int
        get() = shippableItems.sumByFloat { it.shippableItem.quantity }.toInt()
}

sealed interface SelectableShippableItemUI {
    val shippableItem: ShippableItemUI

    data class SingleSelectableShippableItemUI(
        override val shippableItem: ShippableItemUI,
        val isSelected: Boolean = false
    ) : SelectableShippableItemUI

    data class ExpandableSelectableShippableItemUI(
        override val shippableItem: ShippableItemUI,
        val innerShippableItem: ShippableItemUI,
        val isExpanded: Boolean = false,
        val selectedIndexes: Set<Int> = emptySet(),
    ) : SelectableShippableItemUI {
        val isSelected: Boolean
            get() = selectedIndexes.size == shippableItem.quantity.toInt()
    }
}

sealed class SplitShipmentMessage {
    data object Instructions : SplitShipmentMessage()
    data class Success(val snackbarData: ShippingLabelsSnackbarData) : SplitShipmentMessage()
}

data class RemoveShipmentSheet(
    val removingShipments: Map<Int, SelectableShippableItemsUI>,
    val otherShipments: Map<Int, SelectableShippableItemsUI> = emptyMap()
)

private fun List<ShippableItemModel>.combine(other: List<ShippableItemModel>) = (this + other)
    .groupBy { it.itemId }
    .map { (_, itemsWithSameId) -> itemsWithSameId.first().copy(quantity = itemsWithSameId.sumByFloat { it.quantity }) }
