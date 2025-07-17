package com.woocommerce.android.ui.orders.wooshippinglabels.customs

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.woocommerce.android.R
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.UiString
import com.woocommerce.android.ui.orders.wooshippinglabels.address.GetAllCountries
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.domain.ValidateITN
import com.woocommerce.android.ui.orders.wooshippinglabels.customs.products.WooShippingCustomsProductUIModel
import com.woocommerce.android.ui.orders.wooshippinglabels.models.ShippableItemModel
import com.woocommerce.android.util.CoroutineDispatchers
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
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class WooShippingCustomsFormViewModel @Inject constructor(
    private val getAllCountries: GetAllCountries,
    private val validateITN: ValidateITN,
    private val dispatchers: CoroutineDispatchers,
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: WooShippingCustomsFormFragmentArgs by savedState.navArgs()
    private val destinationCountryCode = navArgs.destinationCountryCode

    private val _viewState = savedState.getStateFlow(
        scope = viewModelScope,
        initialValue = initViewState()
    )
    val viewState = _viewState.asLiveData()

    private var possibleLocations: List<Location>? = null
    private var itemIndexUnderCountrySelection: Int? = null

    init {
        launch { loadCountries() }
        monitorITNValidationStatus()
    }

    private fun initViewState(): ViewState {
        return navArgs.customsData?.let { customData ->
            loadViewStateFromExistentCustomData(customData)
        } ?: run {
            val shippableProducts = navArgs.shippableItems.map { item -> item.toProductUIModel() }
            ViewState().copy(shippingProducts = shippableProducts)
        }
    }

    private fun monitorITNValidationStatus() {
        fun ValidateITN.ITNMissingCause.errorMessage() = when (this) {
            ValidateITN.ITNMissingCause.TotalValue ->
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_total_value)

            is ValidateITN.ITNMissingCause.HSTariffValue ->
                UiString.UiStringRes(
                    stringRes = R.string.woo_shipping_labels_customs_itn_required_hs_tariff_value,
                    params = listOf(UiString.UiStringText(this.hsTariffNumber))
                )

            ValidateITN.ITNMissingCause.DestinationCountry ->
                UiString.UiStringRes(R.string.woo_shipping_labels_customs_itn_required_destination_country)
        }

        _viewState.map { it.asCustomData }
            .map { customsData ->
                validateITN(customsData, destinationCountryCode)
            }
            .flowOn(dispatchers.computation)
            .onEach { validationResult ->
                _viewState.update {
                    val itnValue = it.itnValue.currentInput
                    it.copy(
                        itnValue = when (validationResult) {
                            ValidateITN.ITNValidationResult.Valid -> InputValue.Data(itnValue)
                            is ValidateITN.ITNValidationResult.Missing -> InputValue.Error(
                                input = itnValue,
                                errorMessageId = validationResult.cause.errorMessage()
                            )

                            ValidateITN.ITNValidationResult.InvalidFormat -> InputValue.Error(
                                input = itnValue,
                                errorMessageId = R.string.woo_shipping_labels_customs_itn_error_message
                            )
                        }
                    )
                }
            }
            .launchIn(viewModelScope)
    }

    private fun loadViewStateFromExistentCustomData(customData: CustomsData): ViewState {
        return ViewState(
            contentType = customData.contentType,
            otherContentInput = InputValue.Data(customData.contentDescription),
            restrictionType = customData.restrictionType,
            otherRestrictionInput = InputValue.Data(customData.restrictionDescription),
            itnValue = InputValue.Data(customData.itn),
            returnToSenderChecked = customData.isReturnToSender,
            shippingProducts = customData.items.map { item ->
                WooShippingCustomsProductUIModel(
                    productId = item.productID,
                    name = item.description,
                    description = InputValue.Data(item.description),
                    tariffNumber = InputValue.Data(item.hsTariffNumber),
                    valuePerUnit = InputValue.Data(item.value.toString()),
                    weightPerUnit = InputValue.Data(item.weight.toString()),
                    originCountry = item.originCountry,
                    originCountryCode = item.originCountryCode,
                    quantity = item.quantity,
                    isExpanded = false
                )
            }
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
        val input = when (newValue.isBlank()) {
            false -> InputValue.Data(newValue)
            true -> InputValue.Error(
                input = newValue,
                errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
            )
        }
        _viewState.update {
            it.copy(otherContentInput = input)
        }
    }

    fun onRestrictionDetailsInputChanged(newValue: String) {
        val input = when (newValue.isBlank()) {
            false -> InputValue.Data(newValue)
            true -> InputValue.Error(
                input = newValue,
                errorMessageId = R.string.woo_shipping_labels_customs_other_error_message
            )
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
            when (newValue.isBlank()) {
                false -> InputValue.Data(newValue)
                true -> InputValue.Error(
                    input = newValue,
                    errorMessageId = R.string.woo_shipping_labels_customs_product_details_description_missing
                )
            }.let { item.copy(description = it) }
        }
    }

    fun onShippableProductTariffNumberChanged(itemIndex: Int, newValue: String) {
        fun String.isValidHSTariffNumber(): Boolean {
            val regex = Regex("""^(\d{1,2}\.?){3,6}$""")
            if (!regex.matches(this)) return false

            val digits = replace(Regex("""\D"""), "")
            return digits.length in 6..12
        }

        updateShippingProductsAt(itemIndex) { item ->
            when (newValue.isBlank() || newValue.isValidHSTariffNumber()) {
                true -> InputValue.Data(newValue)
                else -> InputValue.Error(
                    input = newValue,
                    errorMessageId = R.string.woo_shipping_labels_customs_product_details_tariff_invalid
                )
            }.let { item.copy(tariffNumber = it) }
        }
    }

    fun onShippableProductValuePerUnitChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            when (newValue.isBlank()) {
                false -> InputValue.Data(newValue)
                true -> newValue.asInputValueError
            }.let { item.copy(valuePerUnit = it) }
        }
    }

    fun onShippableProductWeightPerUnitChanged(itemIndex: Int, newValue: String) {
        updateShippingProductsAt(itemIndex) { item ->
            when (newValue.isBlank()) {
                false -> InputValue.Data(newValue)
                true -> newValue.asInputValueError
            }.let { item.copy(weightPerUnit = it) }
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

    private fun ShippableItemModel.toProductUIModel() = WooShippingCustomsProductUIModel(
        productId = productId,
        name = title,
        description = "".asInputValueError,
        tariffNumber = InputValue.Data(""),
        quantity = quantity,
        originCountry = "",
        originCountryCode = "",
        isExpanded = false,
        valuePerUnit = when {
            price == BigDecimal.ZERO -> InputValue.Error(
                input = "",
                errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
            )

            else -> InputValue.Data(shippingTotalValue.toString())
        },
        weightPerUnit = when {
            weight == 0f -> InputValue.Error(
                input = "",
                errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
            )

            else -> InputValue.Data(weight.toString())
        }
    )

    private val String.asInputValueError
        get() = InputValue.Error(
            input = this,
            errorMessageId = R.string.woo_shipping_labels_customs_product_details_value_required
        )

    @Parcelize
    data class ViewState(
        val contentType: ContentType = ContentType.MERCHANDISE,
        val otherContentInput: InputValue = InputValue.Empty,
        val restrictionType: RestrictionType = RestrictionType.NONE,
        val otherRestrictionInput: InputValue = InputValue.Empty,
        val itnValue: InputValue = InputValue.Empty,
        val returnToSenderChecked: Boolean = false,
        val shippingProducts: List<WooShippingCustomsProductUIModel> = emptyList()
    ) : Parcelable {
        val shouldDisplayContentTypeInput: Boolean
            get() = contentType == ContentType.OTHER

        val shouldDisplayRestrictionTypeInput: Boolean
            get() = restrictionType == RestrictionType.OTHER

        val isAddCustomsButtonEnabled: Boolean
            get() = itnValue is InputValue.Data &&
                (contentType != ContentType.OTHER || otherContentInput is InputValue.Data) &&
                (restrictionType != RestrictionType.OTHER || otherRestrictionInput is InputValue.Data) &&
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
            val errorMessageId: UiString
        ) : InputValue() {
            constructor(input: String, errorMessageId: Int) : this(
                input = input,
                errorMessageId = UiString.UiStringRes(errorMessageId)
            )
        }

        data object Empty : InputValue()

        val currentInput
            get() = when (this) {
                is Data -> input
                is Error -> input
                is Empty -> ""
            }

        val errorMessageOrNull: UiString?
            get() = run { this as? Error }?.errorMessageId
    }

    data class ShowContentTypeDialog(val currentSelection: ContentType) : MultiLiveEvent.Event()
    data class ShowRestrictionTypeDialog(val currentSelection: RestrictionType) : MultiLiveEvent.Event()
    data class ShowCountrySelector(val countries: List<Location>) : MultiLiveEvent.Event()
    data class FinishCustomsForm(val customData: CustomsData) : MultiLiveEvent.Event()
}
