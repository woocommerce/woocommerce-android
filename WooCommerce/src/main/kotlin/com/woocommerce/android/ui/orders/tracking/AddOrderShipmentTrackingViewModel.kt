package com.woocommerce.android.ui.orders.tracking

import android.content.DialogInterface
import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_SHIPMENT_TRACKING_ADD_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.OrderShipmentTracking
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShipmentTrackingProviders
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import javax.inject.Inject
import org.wordpress.android.fluxc.utils.DateUtils as FluxCDateUtils

@HiltViewModel
class AddOrderShipmentTrackingViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val networkStatus: NetworkStatus,
    private val orderDetailRepository: OrderDetailRepository
) : ScopedViewModel(savedState) {
    private val navArgs: AddOrderShipmentTrackingFragmentArgs by savedState.navArgs()

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
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
        triggerEvent(
            ViewShipmentTrackingProviders(
                orderId = navArgs.orderId,
                selectedProvider = addOrderShipmentTrackingViewState.carrier.name
            )
        )
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

            val onOrderChanged =
                orderDetailRepository.addOrderShipmentTracking(navArgs.orderId, shipmentTracking)
            if (!onOrderChanged.isError) {
                AnalyticsTracker.track(AnalyticsEvent.ORDER_TRACKING_ADD_SUCCESS)
                addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(showLoadingProgress = false)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_added))
                triggerEvent(ExitWithResult(shipmentTracking))
            } else {
                AnalyticsTracker.track(
                    AnalyticsEvent.ORDER_TRACKING_ADD_FAILED,
                    prepareTracksEventsDetails(onOrderChanged)
                )
                addOrderShipmentTrackingViewState = addOrderShipmentTrackingViewState.copy(showLoadingProgress = false)
                triggerEvent(ShowSnackbar(string.order_shipment_tracking_error))
            }
        }
    }

    @Suppress("ComplexCondition")
    fun onBackButtonPressed(): Boolean {
        return if (addOrderShipmentTrackingViewState.carrier.name.isNotEmpty() ||
            addOrderShipmentTrackingViewState.trackingNumber.isNotEmpty() ||
            (
                addOrderShipmentTrackingViewState.carrier.isCustom &&
                    addOrderShipmentTrackingViewState.trackingLink.isNotEmpty()
                )
        ) {
            triggerEvent(
                ShowDialog.buildDiscardDialogEvent(
                    positiveBtnAction = DialogInterface.OnClickListener { _, _ ->
                        triggerEvent(Exit)
                    }
                )
            )
            false
        } else {
            true
        }
    }

    private fun prepareTracksEventsDetails(event: OnOrderChanged) = mapOf(
        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
        AnalyticsTracker.KEY_ERROR_TYPE to event.error.type.toString(),
        AnalyticsTracker.KEY_ERROR_DESC to event.error.message
    )

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
}
