package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreateCustomPackageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val shippingLabelRepository: ShippingLabelRepository
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreateCustomPackageViewState())
    private var viewState by viewStateData

    private val stringInputError = R.string.shipping_label_create_custom_package_field_name_hint
    private val floatInputError = R.string.shipping_label_create_custom_package_field_empty_hint

    fun onCustomPackageTypeSelected(selectedPackageType: CustomPackageType) {
        viewState = viewState.copy(customFormType = selectedPackageType)
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

    fun onCustomFormDoneMenuClicked(type: String, name: String,
                                    length: String, width: String,
                                    height: String, weight: String) {
        // Sanitize and validate all input-related fields one last time.
        val lengthF = inputToFloatOrZero(length)
        val widthF = inputToFloatOrZero(width)
        val heightF = inputToFloatOrZero(height)
        val weightF = inputToFloatOrZero(weight)

        viewState = viewState.copy(
            customFormNameError = if(name.isBlank()) stringInputError else null,
            customFormLengthError = if(lengthF.equals(0f)) floatInputError else null,
            customFormWidthError = if(widthF.equals(0f)) floatInputError else null,
            customFormHeightError = if(heightF.equals(0f)) floatInputError else null,
            customFormWeightError = if(weightF.equals(0f)) floatInputError else null
        )

        // At this point, if there's no errors, we assume everything is valid and good to go.
        if(viewState.areAllRequiredFieldsValid) {
            val packageToCreate = ShippingPackage(
                id = "", /* Safe to set as empty, as it's not used for package creation */
                title = name,
                isLetter = type == resourceProvider.getString(CustomPackageType.ENVELOPE.stringRes),
                category = "", /* Safe to set as empty, as it's not used for package creation */
                dimensions = PackageDimensions(lengthF, widthF, heightF),
                boxWeight = weightF
            )
            viewState = viewState.copy(customPackage = packageToCreate)

/*
            // TODO: Add success/error handling. For now we assume it's always successful
            launch {
                shippingLabelRepository.createCustomPackage(packageToCreate)
            }
 */
            triggerEvent(PackageSuccessfullyMadeEvent(packageToCreate))
        }
    }

    private fun inputToFloatOrZero(input: String) : Float {
        return if(input.isBlank()) 0f else input.trim('.').toFloat()
    }

    @Parcelize
    data class ShippingLabelCreateCustomPackageViewState(
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

    enum class InputName {
        NAME,
        LENGTH,
        WIDTH,
        HEIGHT,
        EMPTY_WEIGHT
    }

    data class PackageSuccessfullyMadeEvent(val madePackage: ShippingPackage) : MultiLiveEvent.Event()
}
