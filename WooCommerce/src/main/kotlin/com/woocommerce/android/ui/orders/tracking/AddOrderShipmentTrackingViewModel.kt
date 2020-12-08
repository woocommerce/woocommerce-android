package com.woocommerce.android.ui.orders.tracking

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailViewModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.utils.DateUtils as FluxCDateUtils

class AddOrderShipmentTrackingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val resourceProvider: ResourceProvider
) : ScopedViewModel(savedState, dispatchers) {

    private val navArgs: AddOrderShipmentTrackingFragmentArgs by savedState.navArgs()

    val addOrderShipmentTrackingViewStateData = LiveDataDelegate(
        savedState = savedState,
        initialValue = ViewState(
            isSelectedProviderCustom = navArgs.isCustomProvider,
            carrier = Carrier("", navArgs.isCustomProvider)
        )
    )
    private var addOrderShipmentTrackingViewState by addOrderShipmentTrackingViewStateData

    val currentSelectedDate: String
        get() = addOrderShipmentTrackingViewState.date

    fun onCarrierSelected(carrier: Carrier) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(carrier = carrier)
    }

    fun onCustomCarrierNameEntered(name: String) {
        val carrier = addOrderShipmentTrackingViewState.carrier.copy(name = name)
        onCarrierSelected(carrier)
    }

    fun onTrackingNumberEntered(trackingNumber: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(trackingNumber = trackingNumber)
    }

    fun onTrackingLinkEntered(trackingLink: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(trackingLink = trackingLink)
    }

    fun onDateChanged(date: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(date = date)
    }

    @Parcelize
    data class ViewState(
        val isSelectedProviderCustom: Boolean,
        val carrier: Carrier,
        val trackingNumber: String = "",
        val trackingLink: String? = null,
        val date: String = FluxCDateUtils.getCurrentDateString(),
        val showLoadingProgress: Boolean = false
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<OrderDetailViewModel>
}