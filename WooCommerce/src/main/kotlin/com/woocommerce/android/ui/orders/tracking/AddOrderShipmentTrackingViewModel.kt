package com.woocommerce.android.ui.orders.tracking

import android.content.DialogInterface
import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Stat.ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShipmentTrackingProviders
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.launch
import org.wordpress.android.fluxc.model.order.OrderIdentifier
import org.wordpress.android.fluxc.utils.DateUtils as FluxCDateUtils

class AddOrderShipmentTrackingViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    dispatchers: CoroutineDispatchers,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState, dispatchers) {
    private val navArgs: AddOrderShipmentTrackingFragmentArgs by savedState.navArgs()

    val addOrderShipmentTrackingViewStateData = LiveDataDelegate(
        savedState = savedState,
        initialValue = ViewState(
            isSelectedProviderCustom = navArgs.isCustomProvider,
            carrier = Carrier(navArgs.orderTrackingProvider ?: "", navArgs.isCustomProvider)
        )
    )
    private var addOrderShipmentTrackingViewState by addOrderShipmentTrackingViewStateData

    val currentSelectedDate: String
        get() = addOrderShipmentTrackingViewState.date

    val orderId: OrderIdentifier
        get() = navArgs.orderId

    fun onCarrierSelected(carrier: Carrier) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(
            carrier = carrier,
            trackingLink = if (!carrier.isCustom) "" else addOrderShipmentTrackingViewState.trackingLink,
            carrierError = null
        )
    }

    fun onCustomCarrierNameEntered(name: String) {
        val carrier = addOrderShipmentTrackingViewState.carrier.copy(name = name)
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(
            carrier = carrier,
            customCarrierNameError = null
        )
    }

    fun onTrackingNumberEntered(trackingNumber: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(
            trackingNumber = trackingNumber,
            trackingNumberError = null
        )
    }

    fun onTrackingLinkEntered(trackingLink: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(trackingLink = trackingLink)
    }

    fun onDateChanged(date: String) {
        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(date = date)
    }

    fun onCarrierClicked() {
        triggerEvent(ViewShipmentTrackingProviders(
            orderIdentifier = orderId,
            selectedProvider = addOrderShipmentTrackingViewState.carrier.name
        ))
    }

    fun onAddButtonTapped() {
        if (addOrderShipmentTrackingViewState.carrier.name.isEmpty()) {
            addOrderShipmentTrackingViewState = if (!addOrderShipmentTrackingViewState.carrier.isCustom) {
                addOrderShipmentTrackingViewState.copy(
                    carrierError = R.string.order_shipment_tracking_empty_provider
                )
            } else {
                addOrderShipmentTrackingViewState.copy(
                    customCarrierNameError = R.string.order_shipment_tracking_empty_custom_provider_name
                )
            }
            return
        }
        if (addOrderShipmentTrackingViewState.trackingNumber.isEmpty()) {
            addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(
                trackingNumberError = R.string.order_shipment_tracking_empty_tracking_num
            )
            return
        }

        AnalyticsTracker.track(ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED)
        triggerEvent(SaveTrackingPrefsEvent(addOrderShipmentTrackingViewState.carrier))

        if (!networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(string.offline_error))
            return
        }

        addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(showLoadingProgress = true)

        launch {
            val shipmentTracking = OrderShipmentTracking(
                trackingNumber = addOrderShipmentTrackingViewState.trackingNumber,
                dateShipped = addOrderShipmentTrackingViewState.date,
                trackingProvider = addOrderShipmentTrackingViewState.carrier.name,
                isCustomProvider = addOrderShipmentTrackingViewState.carrier.isCustom,
                trackingLink = addOrderShipmentTrackingViewState.trackingLink
            )

            if (orderDetailRepository.addOrderShipmentTracking(orderId, shipmentTracking)) {
                addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(showLoadingProgress = false)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_added))
                triggerEvent(ExitWithResult(shipmentTracking))
            } else {
                addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(showLoadingProgress = false)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_error))
            }
        }
    }

    fun onBackButtonPressed(): Boolean {
        return if (addOrderShipmentTrackingViewState.carrier.name.isNotEmpty() ||
            addOrderShipmentTrackingViewState.trackingNumber.isNotEmpty() ||
            (addOrderShipmentTrackingViewState.carrier.isCustom &&
                addOrderShipmentTrackingViewState.trackingLink.isNotEmpty())) {
            triggerEvent(ShowDialog.buildDiscardDialogEvent(
                positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                    triggerEvent(Exit)
                }
            ))
            false
        } else {
            true
        }
    }

    override fun onCleared() {
        super.onCleared()
        orderDetailRepository.onCleanup()
    }

    @Parcelize
    data class ViewState(
        val isSelectedProviderCustom: Boolean,
        val carrier: Carrier,
        val trackingNumber: String = "",
        val trackingLink: String = "",
        val date: String = FluxCDateUtils.getCurrentDateString(),
        val showLoadingProgress: Boolean = false,
        val carrierError: Int? = null,
        val customCarrierNameError: Int? = null,
        val trackingNumberError: Int? = null
    ) : Parcelable

    data class SaveTrackingPrefsEvent(val carrier: Carrier) : MultiLiveEvent.Event()

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<AddOrderShipmentTrackingViewModel>
}
