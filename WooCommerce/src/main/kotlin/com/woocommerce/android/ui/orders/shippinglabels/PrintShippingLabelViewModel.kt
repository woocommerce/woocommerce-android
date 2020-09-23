package com.woocommerce.android.ui.orders.shippinglabels

import android.os.Parcelable
import com.squareup.inject.assisted.Assisted
import com.squareup.inject.assisted.AssistedInject
import com.woocommerce.android.di.ViewModelAssistedFactory
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewShippingLabelPaperSizes
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelPaperSizeSelectorDialog.ShippingLabelPaperSize
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.SavedStateWithArgs
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.android.parcel.Parcelize

class PrintShippingLabelViewModel @AssistedInject constructor(
    @Assisted savedState: SavedStateWithArgs,
    private val repository: ShippingLabelRepository,
    private val networkStatus: NetworkStatus,
    dispatchers: CoroutineDispatchers
) : ScopedViewModel(savedState, dispatchers) {
    val viewStateData = LiveDataDelegate(savedState, PrintShippingLabelViewState())
    private var viewState by viewStateData

    fun onPaperSizeOptionsSelected() {
        triggerEvent(ViewShippingLabelPaperSizes(viewState.paperSize))
    }

    fun onPaperSizeSelected(paperSize: ShippingLabelPaperSize) {
        viewState = viewState.copy(paperSize = paperSize)
    }

    @Parcelize
    data class PrintShippingLabelViewState(
        val paperSize: ShippingLabelPaperSize = ShippingLabelPaperSize.LABEL
    ) : Parcelable

    @AssistedInject.Factory
    interface Factory : ViewModelAssistedFactory<PrintShippingLabelViewModel>
}
