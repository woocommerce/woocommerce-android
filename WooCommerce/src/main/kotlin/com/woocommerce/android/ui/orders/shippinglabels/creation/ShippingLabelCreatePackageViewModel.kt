package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize

class ShippingLabelCreatePackageViewModel(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState)  {
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreatePackageViewState())
    private var viewState by viewStateData

    private val stringInputError = R.string.shipping_label_create_custom_package_field_name_hint
    private val floatInputError = R.string.shipping_label_create_custom_package_field_empty_hint


    fun onCustomPackageTypeSelected(selectedPackageType: CustomPackageType) {
        viewState = viewState.copy(customFormType = selectedPackageType)
    }

    fun onCreateCustomPackageDoneMenuClicked(input: ShippingPackage) {
        // Validate all input-related fields one last time.
        viewState = viewState.copy(
            customFormNameError = if(input.title.isBlank()) stringInputError else null,
            customFormLengthError = if(input.dimensions.length.equals(0f)) floatInputError else null,
            customFormWidthError = if(input.dimensions.width.equals(0f)) floatInputError else null,
            customFormHeightError = if(input.dimensions.height.equals(0f)) floatInputError else null,
            customFormWeightError = if(input.boxWeight.equals(0f)) floatInputError else null
        )

        // At this point, if there's no errors, we assume everything is valid.
        if(viewState.areAllRequiredFieldsValid) {
            viewState = viewState.copy(customPackage = input)
            // TODO Save the data to API.
        }
    }

    fun onCustomPackageStringInputChanged(input: String) {
        viewState = if(input.isBlank()) {
            viewState.copy(customFormNameError = stringInputError)
        }
        else {
            viewState.copy(customFormNameError = null)
        }
    }

    fun onCustomPackageFloatInputChanged(input: String, name: InputName) {
        val inputInFloat = if(input.isBlank()) 0f else input.trim('.').toFloat()
        if(inputInFloat.equals(0f)) {
            viewState = when(name) {
                InputName.LENGTH -> viewState.copy(customFormLengthError = floatInputError)
                InputName.WIDTH -> viewState.copy(customFormWidthError = floatInputError)
                InputName.HEIGHT -> viewState.copy(customFormHeightError = floatInputError)
                InputName.EMPTY_WEIGHT -> viewState.copy(customFormWeightError = floatInputError)
                else -> { viewState } /* Do nothing */
            }
        }
        else {
            viewState = when(name) {
                InputName.LENGTH -> viewState.copy(customFormLengthError = null)
                InputName.WIDTH -> viewState.copy(customFormWidthError = null)
                InputName.HEIGHT -> viewState.copy(customFormHeightError = null)
                InputName.EMPTY_WEIGHT -> viewState.copy(customFormWeightError = null)
                else -> { viewState } /* Do nothing */
            }
        }
    }

    @Parcelize
    data class ShippingLabelCreatePackageViewState(
        val customPackage: ShippingPackage? = null, // TODO: for data submission
        val customFormType: CustomPackageType = CustomPackageType.BOX,
        val customFormNameError: Int? = null,
        val customFormLengthError: Int? = null,
        val customFormWidthError: Int? = null,
        val customFormHeightError: Int? = null,
        val customFormWeightError: Int? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val areAllRequiredFieldsValid
            get() = customFormNameError == null && customFormLengthError == null &&
                customFormWidthError == null && customFormHeightError ==  null &&
                customFormWeightError == null
    }

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
