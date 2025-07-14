package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R.string
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
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

    /**
     * Saving more data than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can replace @Suppress("OPT_IN_USAGE")
     * with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ShippingLabelCreateCustomPackageViewState())
    private var viewState by viewStateData

    private val emptyInputError = string.shipping_label_create_custom_package_field_empty_hint
    private val invalidInputError = string.shipping_label_create_custom_package_field_invalid_hint

    private val parameters = parameterRepository.getParameters(KEY_PARAMETERS, savedState)
    val weightUnit = parameters.weightUnit ?: ""
    val dimensionUnit = parameters.dimensionUnit ?: ""

    fun onCustomPackageTypeSelected(selectedPackageType: CustomPackageType) {
        viewState = viewState.copy(type = selectedPackageType)
    }

    fun onFieldTextChanged(value: String, field: InputName) {
        when (field) {
            InputName.NAME -> validateNameField(value)
            InputName.EMPTY_WEIGHT -> validateFloatInput(value, field, isZeroAllowed = true)
            else -> validateFloatInput(value, field)
        }
        updateInputInViewState(value, field)
    }

    private fun validateNameField(input: String) {
        if (input.isBlank()) {
            updateErrorInViewState(InputName.NAME, emptyInputError)
        } else {
            updateErrorInViewState(InputName.NAME, null)
        }
    }

    private fun validateFloatInput(input: String, name: InputName, isZeroAllowed: Boolean = false) {
        val acc = inputToFloat(input)
        when {
            acc.isNaN() -> updateErrorInViewState(name, emptyInputError)
            acc == 0f -> {
                if (isZeroAllowed) {
                    updateErrorInViewState(name, null)
                } else {
                    updateErrorInViewState(name, invalidInputError)
                }
            }
            else -> updateErrorInViewState(name, null)
        }
    }

    private fun updateInputInViewState(input: String, field: InputName) {
        viewState = when (field) {
            InputName.LENGTH -> viewState.copy(length = input)
            InputName.WIDTH -> viewState.copy(width = input)
            InputName.HEIGHT -> viewState.copy(height = input)
            InputName.EMPTY_WEIGHT -> viewState.copy(weight = input)
            InputName.NAME -> viewState.copy(name = input)
        }
    }

    private fun updateErrorInViewState(field: InputName, errorMsg: Int?) {
        viewState = when (field) {
            InputName.LENGTH -> viewState.copy(lengthErrorMessage = errorMsg)
            InputName.WIDTH -> viewState.copy(widthErrorMessage = errorMsg)
            InputName.HEIGHT -> viewState.copy(heightErrorMessage = errorMsg)
            InputName.EMPTY_WEIGHT -> viewState.copy(weightErrorMessage = errorMsg)
            InputName.NAME -> viewState.copy(nameErrorMessage = errorMsg)
        }
    }

    fun onCustomFormDoneMenuClicked() {
        // Sanitize and validate all input-related fields one last time.
        validateNameField(viewState.name)
        validateFloatInput(viewState.length, InputName.LENGTH)
        validateFloatInput(viewState.width, InputName.WIDTH)
        validateFloatInput(viewState.height, InputName.HEIGHT)
        validateFloatInput(viewState.weight, InputName.EMPTY_WEIGHT, isZeroAllowed = true)

        if (!viewState.areAllRequiredFieldsValid) return

        val packageToCreate = ShippingPackage(
            id = "", /* Safe to set as empty, as it's not used for package creation */
            title = viewState.name,
            isLetter = viewState.type.stringRes == CustomPackageType.ENVELOPE.stringRes,
            category = ShippingPackage.CUSTOM_PACKAGE_CATEGORY,
            dimensions = PackageDimensions(
                length = inputToFloat(viewState.length),
                width = inputToFloat(viewState.width),
                height = inputToFloat(viewState.height)
            ),
            boxWeight = inputToFloat(viewState.weight)
        )
        viewState = viewState.copy(customPackage = packageToCreate, isSavingProgressDialogVisible = true)

        launch {
            val result = shippingLabelRepository.createCustomPackage(packageToCreate)
            when {
                result.isError -> {
                    AnalyticsTracker.track(
                        stat = AnalyticsEvent.SHIPPING_LABEL_ADD_PACKAGE_FAILED,
                        properties = mapOf(
                            "type" to "custom",
                            "error" to result.error.message
                        )
                    )

                    val errorMsg = if (result.error.message != null) {
                        result.error.message
                    } else {
                        resourceProvider.getString(string.shipping_label_create_custom_package_api_unknown_failure)
                    }

                    triggerEvent(
                        ShowSnackbar(
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

    private fun inputToFloat(input: String): Float {
        return when {
            input.isBlank() -> Float.NaN
            input == "." -> Float.NaN
            else -> input.toFloat()
        }
    }

    @Parcelize
    data class ShippingLabelCreateCustomPackageViewState(
        val customPackage: ShippingPackage? = null,
        val type: CustomPackageType = CustomPackageType.BOX,
        val name: String = "",
        val length: String = "",
        val width: String = "",
        val height: String = "",
        val weight: String = "",
        val nameErrorMessage: Int? = null,
        val lengthErrorMessage: Int? = null,
        val widthErrorMessage: Int? = null,
        val heightErrorMessage: Int? = null,
        val weightErrorMessage: Int? = null,
        val isSavingProgressDialogVisible: Boolean? = null
    ) : Parcelable {
        @IgnoredOnParcel
        val areAllRequiredFieldsValid
            get() = nameErrorMessage == null && lengthErrorMessage == null &&
                widthErrorMessage == null && heightErrorMessage == null &&
                weightErrorMessage == null
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
