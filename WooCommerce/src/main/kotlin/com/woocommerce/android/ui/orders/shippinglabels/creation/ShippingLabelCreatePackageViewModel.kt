package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.parcelize.Parcelize

class ShippingLabelCreatePackageViewModel(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)  {
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreatePackageViewState())
    private var viewState by viewStateData

    fun onCustomPackageTypeSelected(selectedPackageType: CustomPackageType) {
        viewState = viewState.copy(customPackageType = selectedPackageType)
    }

    fun onCreateCustomPackageDoneButtonClicked(gatherData: ShippingPackage) {
    // TODO: for data submission
    }

    fun onCustomPackageFormLengthChanged(input: String) {
        val inputInFloat = input.trim('.').ifEmpty { null }?.toFloat() ?: Float.NaN
        viewState = if(inputInFloat.isNaN()) {
            viewState.copy(
                customPackageFormLengthError = R.string.shipping_label_create_custom_package_field_empty_hint
            )
        }
        else {
            viewState.copy(customPackageFormLengthError = null)
        }
    }

    enum class PackageType {
        CUSTOM,
        SERVICE
    }

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val createdPackage: ShippingPackage? = null, // TODO: for data submission
        val customPackageType: CustomPackageType = CustomPackageType.BOX,
        val customPackageFormLengthError: Int? = null
    ) : Parcelable
}
