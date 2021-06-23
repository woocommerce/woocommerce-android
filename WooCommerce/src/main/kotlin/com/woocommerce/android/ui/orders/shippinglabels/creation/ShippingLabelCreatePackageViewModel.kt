package com.woocommerce.android.ui.orders.shippinglabels.creation

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.ScopedViewModel

class ShippingLabelCreatePackageViewModel(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)  {
    enum class PackageType {
        CUSTOM,
        SERVICE
    }
}
