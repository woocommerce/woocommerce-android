package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class ShippingLabelRefundViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val repository: ShippingLabelRefundRepository,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
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

    @Parcelize
    data class ShippingLabelRefundViewState(
        val shippingLabel: ShippingLabel? = null
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<ShippingLabelRefundViewModel>
}
