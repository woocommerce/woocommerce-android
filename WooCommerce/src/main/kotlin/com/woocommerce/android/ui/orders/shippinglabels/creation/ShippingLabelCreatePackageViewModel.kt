package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreatePackageViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)  {
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreatePackageViewState())
    private var viewState by viewStateData

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val selectedTab: PackageType = PackageType.CUSTOM
    ) : Parcelable

    enum class PackageType {
        CUSTOM,
        SERVICE
    }
}
