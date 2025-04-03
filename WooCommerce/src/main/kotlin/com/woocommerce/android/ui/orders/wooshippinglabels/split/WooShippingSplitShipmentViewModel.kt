package com.woocommerce.android.ui.orders.wooshippinglabels.split

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import com.woocommerce.android.ui.orders.wooshippinglabels.ShippableItemUI
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
import kotlin.collections.mapValues
import kotlin.getValue

@HiltViewModel
class WooShippingSplitShipmentViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val currencyFormatter: CurrencyFormatter,
    private val getSplitMovements: GetSplitMovements
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingSplitShipmentFragmentArgs by savedState.navArgs()
    private val storeOptions = navArgs.shipmentArgs.storeOptions

    private val currentShipments = MutableStateFlow(navArgs.shipmentArgs.shipments)
    val selectableItems: MutableStateFlow<Map<Int, SelectableShippableItemsUI>?> = MutableStateFlow(null)

    private val shipmentSelected = MutableStateFlow(navArgs.shipmentArgs.shipments.keys.first())
    private val splitMessage: MutableStateFlow<SplitShipmentMessage?> = MutableStateFlow(null)

    init {
        launch {
            delay(NOTIFICATIONS_DELAY)
            splitMessage.value = SplitShipmentMessage.Instructions
        }
        launch {
            currentShipments.collectLatest { shipments ->
                selectableItems.value = shipments.mapValues {
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
        selectableItems.filterNotNull(),
        splitMessage
    ) { shipmentSelected, selectableItems, message ->
        SplitShipmentViewState(
            shipmentSelected = shipmentSelected,
            selectableItems = selectableItems,
            splitMovements = getSplitMovements(
                currentShipment = shipmentSelected,
                shipments = currentShipments.value,
                selection = selectableItems
            ),
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
        shipmentSelected.value = shipmentKey
    }

    fun onUpdateSelection(
        shipmentKey: Int,
        shippableItemIndex: Int,
        selectedIndexes: Set<Int>? = null
    ) {
        val shipmentsMap = selectableItems.value?.toMutableMap() ?: return
        val items = shipmentsMap.getValue(shipmentKey)
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
        shipmentsMap[shipmentKey] = items.copy(shippableItems = updatedList)
        selectableItems.value = shipmentsMap
    }

    fun onUpdateShipment(splitMovement: SplitMovement) {
        currentShipments.update {
            val shipments = it.toMutableMap()
            if (splitMovement.updatedCurrentShipmentItems.isEmpty()) {
                shipments.remove(splitMovement.currentShipment)
            } else {
                shipments[splitMovement.currentShipment] = splitMovement.updatedCurrentShipmentItems
            }
            shipments[splitMovement.updatedShipment] = shipments[splitMovement.updatedShipment]
                ?.takeIf { it.isNotEmpty() }
                ?.combine(splitMovement.updatedShipmentItems)
                ?: splitMovement.updatedShipmentItems
            shipments
        }
    }

    data class SplitShipmentViewState(
        val shipmentSelected: Int,
        val selectableItems: Map<Int, SelectableShippableItemsUI>,
        val splitMovements: List<SplitMovement> = emptyList(),
        val splitMessage: SplitShipmentMessage? = null
    )

    companion object {
        const val NOTIFICATIONS_DELAY = 500L
    }
}

data class SelectableShippableItemsUI(
    val shippableItems: List<SelectableShippableItemUI>,
    val formattedTotalWeight: String,
    val formattedTotalPrice: String
)

sealed class SelectableShippableItemUI {
    data class SingleSelectableShippableItemUI(
        val shippableItem: ShippableItemUI,
        val isSelected: Boolean = false
    ) : SelectableShippableItemUI()

    data class ExpandableSelectableShippableItemUI(
        val shippableItem: ShippableItemUI,
        val innerShippableItem: ShippableItemUI,
        val isExpanded: Boolean = false,
        val selectedIndexes: Set<Int> = emptySet(),
    ) : SelectableShippableItemUI() {
        val isSelected: Boolean
            get() = selectedIndexes.size == shippableItem.quantity.toInt()
    }
}

sealed class SplitShipmentMessage {
    data object Instructions : SplitShipmentMessage()
    data class Success(
        val message: String,
        val action: () -> Unit
    ) : SplitShipmentMessage()
}

fun List<ShippableItemModel>.combine(other: List<ShippableItemModel>): List<ShippableItemModel> {
    val combinedMap = this.associateBy { it.productId }.toMutableMap()
    other.forEach { otherItem ->
        val existingItem = combinedMap[otherItem.productId]
        if (existingItem != null) {
            combinedMap[otherItem.productId] = existingItem.copy(quantity = existingItem.quantity + otherItem.quantity)
        } else {
            combinedMap[otherItem.productId] = otherItem
        }
    }
    return combinedMap.values.toList()
}
