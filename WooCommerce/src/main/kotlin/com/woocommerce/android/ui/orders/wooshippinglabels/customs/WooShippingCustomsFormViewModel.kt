package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAllCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.WooShippingCustomsValidator
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.products.WooShippingCustomsProductUIModel
import com.woocommerce.android.util.CoroutineDispatchers
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import javax.inject.Inject

@HiltViewModel
class WooShippingCustomsFormViewModel @Inject constructor(
    private val getAllCountries: GetAllCountries,
    private val customsValidator: WooShippingCustomsValidator,
    private val dispatchers: CoroutineDispatchers,
    private val currencyFormatter: CurrencyFormatter,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingCustomsFormFragmentArgs by savedState.navArgs()
    private val originCountryCode = navArgs.originCountryCode
    private val destinationCountryCode = navArgs.destinationCountryCode
    private val storeOptions = navArgs.storeOptions

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = loadViewStateFromCustomsData(
            customData = navArgs.customsData,
            currencySymbol = storeOptions.currencySymbol,
            weightUnit = storeOptions.weightUnit
        )
    )
    val viewState = _viewState.asLiveData()

    private var possibleLocations: List<Location>? = null
    private var itemIndexUnderCountrySelection: Int? = null

    init {
        launch { loadCountries() }
        monitorITNValidationStatus()
    }

    private fun monitorITNValidationStatus() {
        _viewState.map { it.asCustomData }
            .map { customsData ->
                customsValidator.validateITN(customsData, destinationCountryCode)
            }
            .flowOn(dispatchers.computation)
            .onEach { validationResult ->
                _viewState.update {
                    val itnValue = it.itnValue.currentInput
                    it.copy(
                        itnValue = when (validationResult) {
                            WooShippingCustomsValidator.FieldValidationResult.Valid -> InputValue.Data(itnValue)
                            is WooShippingCustomsValidator.FieldValidationResult.Invalid ->
                                InputValue.Error(itnValue, validationResult.errorMessage)
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadViewStateFromCustomsData(
        customData: CustomsData,
        currencySymbol: String,
        weightUnit: String
    ): ViewState {
        return ViewState(
            contentType = customData.contentType,
            otherContentInput = validateAsInputValue(customData.contentDescription) {
                customsValidator.validateContentType(customData.contentType, it)
            },
            restrictionType = customData.restrictionType,
            otherRestrictionInput = validateAsInputValue(customData.restrictionDescription) {
                customsValidator.validateRestrictionType(customData.restrictionType, it)
            },
            itnValue = InputValue.Data(customData.itn),
            returnToSenderChecked = customData.isReturnToSender,
            shippingProducts = customData.items.map { item ->
                WooShippingCustomsProductUIModel(
                    productId = item.productID,
                    name = item.description,
                    description = validateAsInputValue(item.description) {
                        customsValidator.validateProductDescription(it, originCountryCode, destinationCountryCode)
                    },
                    tariffNumber = validateAsInputValue(item.hsTariffNumber) {
                        customsValidator.validateHSTariffNumber(it, destinationCountryCode)
                    },
                    valuePerUnit = validateAsInputValue(item.value.toString(), customsValidator::validateProductValue),
                    weightPerUnit = validateAsInputValue(
                        item.weight.takeIf { it != 0f }?.toString() ?: "",
                        customsValidator::validateProductWeight
                    ),
                    originCountry = item.originCountry,
                    originCountryCode = item.originCountryCode,
                    quantity = item.quantity,
                    isExpanded = false,
                    formattedPriceAndWeight = formatPriceAndWeightPerItem(
                        item.value.toString(),
                        item.weight.toString()
                    )
                )
            },
            currencySymbol = currencySymbol,
            weightUnit = weightUnit
        )
    }

    fun onContentTypeClick() {
        val currentSelection = _viewState.value.contentType
        triggerEvent(ShowContentTypeDialog(currentSelection))
    }

    fun onRestrictionTypeClick() {
        val currentSelection = _viewState.value.restrictionType
        triggerEvent(ShowRestrictionTypeDialog(currentSelection))
    }

    fun onReturnToSenderChanged(isChecked: Boolean) {
        _viewState.update {
            it.copy(returnToSenderChecked = isChecked)
        }
    }

    fun onContentTypeSelected(contentType: ContentType) {
        _viewState.update {
            it.copy(contentType = contentType)
        }
    }

    fun onRestrictionTypeSelected(restrictionType: RestrictionType) {
        _viewState.update {
            it.copy(restrictionType = restrictionType)
        }
    }

    fun onOtherContentInputChanged(newValue: String) {
        val input = validateAsInputValue(newValue) {
            customsValidator.validateContentType(_viewState.value.contentType, it)
        }
        _viewState.update {
            it.copy(otherContentInput = input)
        }
    }

    fun onRestrictionDetailsInputChanged(newValue: String) {
        val input = validateAsInputValue(newValue) {
            customsValidator.validateRestrictionType(_viewState.value.restrictionType, it)
        }
        _viewState.update {
            it.copy(otherRestrictionInput = input)
        }
    }

    fun onITNChanged(newItnValue: String) {
        _viewState.update { it.copy(itnValue = InputValue.Data(newItnValue)) }
    }

    fun onShippableProductExpanded(itemIndex: Int, isExpanded: Boolean) {
        _viewState.update { state ->
            val updatedItem = state.shippingProducts[itemIndex]
                .copy(isExpanded = isExpanded)

            state.shippingProducts.toMutableList().apply {
                set(itemIndex, updatedItem)
            }.let { state.copy(shippingProducts = it) }
        }
    }

    fun onShippableProductDescriptionChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            item.copy(
                description = validateAsInputValue(newValue) {
                    customsValidator.validateProductDescription(it, originCountryCode, destinationCountryCode)
                }
            )
        }
    }

    fun onShippableProductTariffNumberChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            item.copy(
                tariffNumber = validateAsInputValue(newValue) {
                    customsValidator.validateHSTariffNumber(it, destinationCountryCode)
                }
            )
        }
    }

    fun onShippableProductValuePerUnitChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            validateAsInputValue(newValue, customsValidator::validateProductValue).let {
                item.copy(
                    valuePerUnit = it,
                    formattedPriceAndWeight = formatPriceAndWeightPerItem(
                        it.currentInput,
                        item.weightPerUnit.currentInput
                    )
                )
            }
        }
    }

    fun onShippableProductWeightPerUnitChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            validateAsInputValue(newValue, customsValidator::validateProductWeight).let {
                item.copy(
                    weightPerUnit = it,
                    formattedPriceAndWeight = formatPriceAndWeightPerItem(
                        item.valuePerUnit.currentInput,
                        it.currentInput
                    )
                )
            }
        }
    }

    fun onCountrySelectorClick(itemIndex: Int) {
        itemIndexUnderCountrySelection = itemIndex

        possibleLocations?.let { triggerEvent(ShowCountrySelector(it)) }
    }

    fun onShippableProductOriginCountryChanged(newValue: String) {
        val itemIndex = itemIndexUnderCountrySelection ?: return
        itemIndexUnderCountrySelection = null

        val selectedLocation = possibleLocations
            ?.firstOrNull { it.code == newValue }
            ?: AmbiguousLocation.Raw(newValue).asLocation()

        _viewState.update { state ->
            val updatedItem = state.shippingProducts[itemIndex].copy(
                originCountry = selectedLocation.name,
                originCountryCode = selectedLocation.code
            )

            state.shippingProducts.toMutableList().apply {
                set(itemIndex, updatedItem)
            }.let { state.copy(shippingProducts = it) }
        }
    }

    fun onAddCustomsDataClick() {
        _viewState.value.asCustomData.let { triggerEvent(FinishCustomsForm(it)) }
    }

    private fun updateShippingProductsAt(
        itemIndex: Int,
        generateUpdatedItem: (WooShippingCustomsProductUIModel) -> WooShippingCustomsProductUIModel
    ) {
        _viewState.update { state ->
            val updatedItem = state.shippingProducts[itemIndex]
                .let(generateUpdatedItem)

            state.shippingProducts.toMutableList().apply {
                set(itemIndex, updatedItem)
            }.let { state.copy(shippingProducts = it) }
        }
    }

    private suspend fun loadCountries() {
        getAllCountries().fold(
            onSuccess = { possibleLocations = it },
            onFailure = { possibleLocations = null }
        )
    }

    private fun validateAsInputValue(
        input: String,
        validate: (String) -> WooShippingCustomsValidator.FieldValidationResult
    ): InputValue = when (val validationResult = validate(input)) {
        is WooShippingCustomsValidator.FieldValidationResult.Invalid ->
            InputValue.Error(input, validationResult.errorMessage)

        WooShippingCustomsValidator.FieldValidationResult.Valid ->
            InputValue.Data(input)
    }

    private fun formatPriceAndWeightPerItem(price: String, weight: String): String =
        "${currencyFormatter.formatCurrency(price, storeOptions.currencySymbol)} " +
            "• $weight${storeOptions.weightUnit}"

    @Parcelize
    data class ViewState(
        val contentType: ContentType = ContentType.MERCHANDISE,
        val otherContentInput: InputValue = InputValue.Empty,
        val restrictionType: RestrictionType = RestrictionType.NONE,
        val otherRestrictionInput: InputValue = InputValue.Empty,
        val itnValue: InputValue = InputValue.Empty,
        val returnToSenderChecked: Boolean = false,
        val shippingProducts: List<WooShippingCustomsProductUIModel> = emptyList(),
        val currencySymbol: String,
        val weightUnit: String,
    ) : Parcelable {
        val shouldDisplayContentTypeInput: Boolean
            get() = contentType == ContentType.OTHER

        val shouldDisplayRestrictionTypeInput: Boolean
            get() = restrictionType == RestrictionType.OTHER

        val isAddCustomsButtonEnabled: Boolean
            get() = itnValue.isValid &&
                (contentType != ContentType.OTHER || otherContentInput.isValid) &&
                (restrictionType != RestrictionType.OTHER || otherRestrictionInput.isValid) &&
                shippingProducts.all { it.isValid }

        val asCustomData: CustomsData
            get() = CustomsData(
                contentType = contentType,
                contentDescription = otherContentInput.currentInput,
                restrictionType = restrictionType,
                restrictionDescription = otherRestrictionInput.currentInput,
                itn = itnValue.currentInput,
                isReturnToSender = returnToSenderChecked,
                items = shippingProducts.map { it.asCustomItem }
            )
    }

    @Parcelize
    sealed class InputValue : Parcelable {
        data class Data(val input: String) : InputValue()
        data class Error(
            val input: String,
            val errorMessage: UiString
        ) : InputValue() {
            constructor(input: String, errorMessageId: Int) : this(
                input = input,
                errorMessage = UiString.UiStringRes(errorMessageId)
            )
        }

        data object Empty : InputValue()

        val isValid: Boolean
            get() = this is Data

        val currentInput
            get() = when (this) {
                is Data -> input
                is Error -> input
                is Empty -> ""
            }

        val errorMessageOrNull: UiString?
            get() = run { this as? Error }?.errorMessage
    }

    data class ShowContentTypeDialog(val currentSelection: ContentType) : MultiLiveEvent.Event()
    data class ShowRestrictionTypeDialog(val currentSelection: RestrictionType) : MultiLiveEvent.Event()
    data class ShowCountrySelector(val countries: List<Location>) : MultiLiveEvent.Event()
    data class FinishCustomsForm(val customData: CustomsData) : MultiLiveEvent.Event()
}
