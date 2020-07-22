package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.R.string
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ShippingLabelRefundViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val repository: ShippingLabelRefundRepository,
    private val networkStatus: NetworkStatus,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    private var refundJob: Job? = null
    final val isRefundInProgress: Boolean
        get() = refundJob?.isActive ?: false

    private val arguments: ShippingLabelRefundFragmentArgs by savedState.navArgs()

    final val shippingLabelRefundViewStateData = LiveDataDelegate(savedState, ShippingLabelRefundViewState())
    private var shippingLabelRefundViewState by shippingLabelRefundViewStateData

    fun start() {
        shippingLabelRefundViewState = shippingLabelRefundViewState.copy(
            shippingLabel = repository.getShippingLabelByOrderIdAndLabelId(
                arguments.orderId, arguments.shippingLabelId
            )
        )
    }

    fun onRefundShippingLabelButtonClicked() {
        if (networkStatus.isConnected()) {
            triggerEvent(ShowSnackbar(string.shipping_label_refund_progress_message))

            refundJob = launch {
                val result = repository.refundShippingLabel(arguments.orderId, arguments.shippingLabelId)
                if (result.isError) {
                    // TODO: add tracking event to track the failure when refunding shipping label
                    triggerEvent(ShowSnackbar(string.order_refunds_amount_refund_error))
                } else {
                    // TODO: add tracking event to track the success when refunding shipping label
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
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingLabelRefundViewModel>
}
