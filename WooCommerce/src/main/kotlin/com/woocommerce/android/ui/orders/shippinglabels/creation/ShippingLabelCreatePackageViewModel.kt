package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelCustomPackageTypeDialog.ShippingLabelCustomPackageType
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.parcelize.Parcelize

class ShippingLabelCreatePackageViewModel(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)  {

    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreatePackageViewState())
    private var viewState by viewStateData

    fun onCustomPackageTypeSpinnerSelected() {
        triggerEvent(ViewShippingLabelCustomPackageTypesEvent(viewState.customPackageType))
    }

    fun onCustomPackageTypeSelected(selectedPackageType: ShippingLabelCustomPackageType) {
        viewState = viewState.copy(customPackageType = selectedPackageType)
    }

    enum class PackageType {
        CUSTOM,
        SERVICE
    }

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val customPackageType: ShippingLabelCustomPackageType = ShippingLabelCustomPackageType.BOX
    ) : Parcelable
}

data class ViewShippingLabelCustomPackageTypesEvent(val currentPackageType: ShippingLabelCustomPackageType) : MultiLiveEvent.Event()
