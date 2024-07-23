package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.creation.MoveShippingItemViewModel.DestinationPackage.*
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
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

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    val availableDestinations: List<DestinationPackage>
    val currentPackage = navArgs.currentPackage

    init {
        val availableExistingPackages = navArgs.packagesList
            .filter { it != currentPackage && it.selectedPackage?.isIndividual != true }
            .map { ExistingPackage(it) }

        availableDestinations = availableExistingPackages +
            when {
                currentPackage.selectedPackage?.id == ShippingPackage.INDIVIDUAL_PACKAGE -> listOf(NewPackage)
                currentPackage.items.size == 1 && navArgs.item.quantity == 1 -> listOf(OriginalPackage)
                else -> listOf(NewPackage, OriginalPackage)
            }
    }

    fun onDestinationPackageSelected(destinationPackage: DestinationPackage) {
        viewState = viewState.copy(selectedDestination = destinationPackage)
    }

    fun onMoveButtonClicked() {
        viewState.selectedDestination?.let {
            AnalyticsTracker.track(
                stat = AnalyticsEvent.SHIPPING_LABEL_ITEM_MOVED,
                properties = mapOf(
                    "destination" to when (it) {
                        is ExistingPackage -> "existing_package"
                        NewPackage -> "new_package"
                        OriginalPackage -> "original_packaging"
                    }
                )
            )
            triggerEvent(ExitWithResult(MoveItemResult(navArgs.item, navArgs.currentPackage, it)))
        } ?: throw IllegalStateException("move button listener invoked while no package is selected")
    }

    fun onCancelButtonClicked() {
        triggerEvent(Exit)
    }

    @Parcelize
    data class ViewState(
        val selectedDestination: DestinationPackage? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isMoveButtonEnabled
            get() = selectedDestination != null
    }

    @Parcelize
    data class MoveItemResult(
        val item: ShippingLabelPackage.Item,
        val currentPackage: ShippingLabelPackage,
        val destination: DestinationPackage
    ) : Parcelable

    sealed class DestinationPackage : Parcelable {
        @Parcelize
        object NewPackage : DestinationPackage()

        @Parcelize
        data class ExistingPackage(val destinationPackage: ShippingLabelPackage) : DestinationPackage()

        @Parcelize
        object OriginalPackage : DestinationPackage()
    }
}
