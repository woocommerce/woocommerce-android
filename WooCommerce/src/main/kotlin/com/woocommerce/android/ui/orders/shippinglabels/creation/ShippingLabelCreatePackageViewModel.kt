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

    fun onCustomPackageStringInputChanged(input: String) {
        viewState = if(input.isBlank()) {
            viewState.copy(customPackageFormNameError = R.string.shipping_label_create_custom_package_field_name_hint)
        }
        else {
            viewState.copy(customPackageFormNameError = null)
        }
    }

    fun onCustomPackageFloatInputChanged(input: String, name: InputName) {
        val inputInFloat = input.trim('.').ifEmpty { null }?.toFloat() ?: Float.NaN
        if(inputInFloat.isNaN() or inputInFloat.equals(0f)) {
            val errorMessage = R.string.shipping_label_create_custom_package_field_empty_hint
            when(name) {
                InputName.LENGTH -> viewState = viewState.copy(customPackageFormLengthError = errorMessage)
                InputName.WIDTH -> viewState = viewState.copy(customPackageFormWidthError = errorMessage)
                InputName.HEIGHT -> viewState = viewState.copy(customPackageFormHeightError = errorMessage)
                InputName.EMPTY_WEIGHT -> viewState = viewState.copy(customPackageFormWeightError = errorMessage)
                else -> { /* Nothing to do */ }
            }
        }
        else {
            when(name) {
                InputName.LENGTH -> viewState = viewState.copy(customPackageFormLengthError = null)
                InputName.WIDTH -> viewState = viewState.copy(customPackageFormWidthError = null)
                InputName.HEIGHT -> viewState = viewState.copy(customPackageFormHeightError = null)
                InputName.EMPTY_WEIGHT -> viewState = viewState.copy(customPackageFormWeightError = null)
                else -> { /* Nothing to do */ }
            }
        }
    }

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val createdPackage: ShippingPackage? = null, // TODO: for data submission
        val customPackageType: CustomPackageType = CustomPackageType.BOX,
        val customPackageFormNameError: Int? = null,
        val customPackageFormLengthError: Int? = null,
        val customPackageFormWidthError: Int? = null,
        val customPackageFormHeightError: Int? = null,
        val customPackageFormWeightError: Int? = null
    ) : Parcelable

    enum class PackageType {
        CUSTOM,
        SERVICE
    }

    enum class InputName {
        NAME,
        LENGTH,
        WIDTH,
        HEIGHT,
        EMPTY_WEIGHT
    }
}
