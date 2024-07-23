package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ShippingLabelRefundViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus
) : ScopedViewModel(savedState) {
    private var refundJob: Job? = null
    val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    private val arguments: ShippingLabelRefundFragmentArgs by savedState.navArgs()

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val shippingLabelRefundViewStateData = LiveDataDelegate(savedState, ShippingLabelRefundViewState())
    private var shippingLabelRefundViewState by shippingLabelRefundViewStateData

    init {
        start()
    }

    fun start() {
        shippingLabelRefundViewState = shippingLabelRefundViewState.copy(
            shippingLabel = repository.getShippingLabelByOrderIdAndLabelId(
                arguments.orderId, arguments.shippingLabelId
            )
        )
    }

    fun onRefundShippingLabelButtonClicked() {
        if (networkStatus.isConnected()) {
            AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_REFUND_REQUESTED)
            triggerEvent(ShowSnackbar(string.shipping_label_refund_progress_message))

            refundJob = launch {
                val result = repository.refundShippingLabel(arguments.orderId, arguments.shippingLabelId)
                if (result.isError) {
                    triggerEvent(ShowSnackbar(string.order_refunds_amount_refund_error))
                } else {
                    triggerEvent(ShowSnackbar(string.shipping_label_refund_success))
                    triggerEvent(Exit)
                }
            }
        } else {
            triggerEvent(ShowSnackbar(string.offline_error))
        }
    }

    @Parcelize
    data class ShippingLabelRefundViewState(
        val shippingLabel: ShippingLabel? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val isRefundExpired: Boolean
            get() = shippingLabel?.isAnonymized == true ||
                shippingLabel?.refundExpiryDate?.let { Date().after(it) } ?: false
    }
}
