package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.model.CustomPackageType
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.ui.orders.shippinglabels.ShippingLabelRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class ShippingLabelCreateCustomPackageViewModel @Inject constructor(
    savedState: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val shippingLabelRepository: ShippingLabelRepository,
    parameterRepository: ParameterRepository
) : ScopedViewModel(savedState) {
    companion object {
        private const val KEY_PARAMETERS = "key_parameters"
    }

    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreateCustomPackageViewState())
    private var viewState by viewStateData

    private val emptyInputError = string.shipping_label_create_custom_package_field_empty_hint
    private val invalidInputError = string.shipping_label_create_custom_package_field_invalid_hint

    private val parameters = parameterRepository.getParameters(KEY_PARAMETERS, savedState)
    val weightUnit = parameters.weightUnit ?: ""
    val dimensionUnit = parameters.dimensionUnit ?: ""


    fun onCustomPackageTypeSelected(selectedPackageType: CustomPackageType) {
        viewState = viewState.copy(customFormType = selectedPackageType)
    }

    fun sanitizeStringInput(input: String) {
        if(input.isBlank()) {
            updateErrorViewState(InputName.NAME, emptyInputError)
        }
        else {
            updateErrorViewState(InputName.NAME, null)
        }
    }

    fun sanitizeFloatInput(input: String, name: InputName) {
        val acc = inputToFloat(input)
        when {
            acc.isNaN() -> updateErrorViewState(name, emptyInputError)
            acc == 0f -> updateErrorViewState(name, invalidInputError)
            else -> updateErrorViewState(name, null)
        }
    }

    private fun updateErrorViewState(name: InputName, errorMsg: Int?) {
        viewState = when(name) {
            InputName.LENGTH -> viewState.copy(customFormLengthError = errorMsg)
            InputName.WIDTH -> viewState.copy(customFormWidthError = errorMsg)
            InputName.HEIGHT -> viewState.copy(customFormHeightError = errorMsg)
            InputName.EMPTY_WEIGHT -> viewState.copy(customFormWeightError = errorMsg)
            InputName.NAME ->  viewState.copy(customFormNameError = errorMsg)
        }
    }

    fun onCustomFormDoneMenuClicked(type: String, name: String,
                                    length: String, width: String,
                                    height: String, weight: String) {
        // Sanitize and validate all input-related fields one last time.
        sanitizeStringInput(name)
        sanitizeFloatInput(length, InputName.LENGTH)
        sanitizeFloatInput(width, InputName.WIDTH)
        sanitizeFloatInput(height, InputName.HEIGHT)
        sanitizeFloatInput(weight, InputName.EMPTY_WEIGHT)

        // At this point, if there's no errors, we assume everything is valid and good to go.
        if(viewState.areAllRequiredFieldsValid) {
            val packageToCreate = ShippingPackage(
                id = "", /* Safe to set as empty, as it's not used for package creation */
                title = name,
                isLetter = type == resourceProvider.getString(CustomPackageType.ENVELOPE.stringRes),
                category = ShippingPackage.CUSTOM_PACKAGE_CATEGORY,
                dimensions = PackageDimensions(
                    length = inputToFloat(length),
                    width = inputToFloat(width),
                    height =inputToFloat(height)
                ),
                boxWeight = inputToFloat(weight)
            )
            viewState = viewState.copy(customPackage = packageToCreate, isSavingProgressDialogVisible = true)

            launch {
                val result = shippingLabelRepository.createCustomPackage(packageToCreate)
                when {
                    result.isError -> {
                        val errorMsg = if (result.error.message != null) {
                            result.error.message
                        } else {
                            resourceProvider.getString(string.shipping_label_create_custom_package_api_unknown_failure)
                        }

                        triggerEvent(ShowSnackbar(
                                message = string.shipping_label_create_custom_package_api_failure,
                                args = arrayOf(errorMsg as String)
                           )
                        )
                    }
                    result.model == true -> {
                        triggerEvent(PackageSuccessfullyMadeEvent(packageToCreate))
                    }
                    else -> triggerEvent(ShowSnackbar(string.shipping_label_create_custom_package_api_unknown_failure))
                }
                viewState = viewState.copy(isSavingProgressDialogVisible = false)
            }
        }
        else {
            triggerEvent(ShowSnackbar(string.shipping_label_create_custom_package_generic_failure))
        }
    }

    private fun inputToFloat(input: String) : Float {
        return if(input.isBlank()) Float.NaN else input.trim('.').toFloat()
    }

    @Parcelize
    data class ShippingLabelCreateCustomPackageViewState(
        val customPackage: ShippingPackage? = null, // TODO: for data submission
        val customFormType: CustomPackageType = CustomPackageType.BOX,
        val customFormNameError: Int? = null,
        val customFormLengthError: Int? = null,
        val customFormWidthError: Int? = null,
        val customFormHeightError: Int? = null,
        val customFormWeightError: Int? = null,
        val isSavingProgressDialogVisible: Boolean? = null
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

    data class PackageSuccessfullyMadeEvent(val madePackage: ShippingPackage) : Event()
}
