package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.ExistingPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.NewPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.OriginalPackage
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class MoveShippingItemViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: MoveShippingItemDialogArgs by savedState.navArgs()

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val availableDestinations: List<DestinationPackage>
    val currentPackage = navArgs.currentPackage

    init {
        // TODO
        availableDestinations = listOf(NewPackage, OriginalPackage)
    }

    fun onDestinationPackageSelected(destinationPackage: DestinationPackage) {
        viewState = viewState.copy(selectedDestination = destinationPackage)
    }

    fun onMoveButtonClicked() {
        viewState.selectedDestination?.let {
            moveItem(it)
        } ?: throw IllegalStateException("move button listener invoked while no package is selected")
    }

    private fun moveItem(destination: DestinationPackage) {
        val packages = navArgs.packagesList.toMutableList()
        val item = navArgs.item
        val currentPackage = navArgs.currentPackage

        fun moveItemToNewPackage(): List<ShippingLabelPackage> {
            val updatedItems = if (item.quantity > 1) {
                // if the item quantity is more than one, subtract 1 from it
                val mutableItems = currentPackage.items.toMutableList()
                val updatedItem = item.copy(quantity = item.quantity - 1)
                mutableItems[mutableItems.indexOf(item)] = updatedItem
                mutableItems
            } else {
                currentPackage.items - item
            }
            packages[packages.indexOf(currentPackage)] = currentPackage.copy(items = updatedItems)
            packages.add(
                ShippingLabelPackage(
                    position = packages.size + 1,
                    selectedPackage = currentPackage.selectedPackage,
                    weight = item.weight + (currentPackage.selectedPackage?.boxWeight ?: 0f),
                    items = listOf(item.copy(quantity = 1))
                )
            )
            return packages
        }

        triggerEvent(
            ExitWithResult(
                when (destination) {
                    is ExistingPackage -> TODO()
                    NewPackage -> moveItemToNewPackage()
                    OriginalPackage -> TODO()
                }
            )
        )
    }

    @Parcelize
    data class ViewState(
        val selectedDestination: DestinationPackage? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isMoveButtonEnabled
            get() = selectedDestination != null
    }

    sealed class DestinationPackage : Parcelable {
        @Parcelize
        object NewPackage : DestinationPackage()

        @Parcelize
        class ExistingPackage(val destinationPackage: ShippingLabelPackage) : DestinationPackage()

        @Parcelize
        object OriginalPackage : DestinationPackage()
    }
}
