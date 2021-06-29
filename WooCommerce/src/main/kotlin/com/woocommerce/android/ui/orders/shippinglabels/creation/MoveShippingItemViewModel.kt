package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem.NewPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationItem.OriginalPackage
import com.woocommerce.android.viewmodel.LiveDataDelegate
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

    val availableDestinations: List<DestinationItem>
    val currentPackage = navArgs.currentPackage

    init {
        // TODO
        availableDestinations = listOf(NewPackage, OriginalPackage)
    }

    @Parcelize
    data class ViewState(
        val selectedDestination: DestinationItem? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isMoveButtonEnabled
            get() = selectedDestination != null
    }

    sealed class DestinationItem : Parcelable {
        @Parcelize
        object NewPackage : DestinationItem()

        @Parcelize
        class ExistingPackage(val destinationPackage: ShippingLabelPackage) : DestinationItem()

        @Parcelize
        object OriginalPackage : DestinationItem()
    }
}
