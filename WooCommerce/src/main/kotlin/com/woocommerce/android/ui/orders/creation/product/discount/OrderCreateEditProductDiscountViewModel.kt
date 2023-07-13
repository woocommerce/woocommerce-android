package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getNullableStateFlow
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.updateAndGet
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditProductDiscountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val calculateItemDiscountAmount: CalculateItemDiscountAmount,
    siteParamsRepo: ParameterRepository,
    currencySymbolFinder: CurrencySymbolFinder,
) : ScopedViewModel(savedStateHandle) {
    private val args =
        OrderCreateEditProductDiscountFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val orderItem: MutableStateFlow<Order.Item> =
        savedStateHandle.getStateFlow(scope = this, initialValue = args.item, key = "key_item")

    private val discount = savedStateHandle.getNullableStateFlow(
        scope = this, initialValue = getInitialDiscountAmount(), key = "key_discount", clazz = BigDecimal::class.java
    )
    private val currency = currencySymbolFinder.findCurrencySymbol(args.currency)

    private val currencyFormattingParameters =
        siteParamsRepo.getParameters("key_site_params", savedStateHandle).currencyFormattingParameters
    private val decimalSeparator = currencyFormattingParameters?.currencyDecimalSeparator
        ?: DecimalFormatSymbols(Locale.getDefault()).decimalSeparator.toString()
    private val numberOfDecimals = currencyFormattingParameters?.currencyDecimalNumber
        ?: DEFAULT_DECIMALS_NUMBER

    val discountInputFieldConfig = DiscountInputFieldConfig(
        decimalSeparator = decimalSeparator,
        numberOfDecimals = numberOfDecimals
    )

    private fun getInitialDiscountAmount(): BigDecimal? = with(calculateItemDiscountAmount(args.item)) {
        if (this > BigDecimal.ZERO) this else null
    }

    val viewState: StateFlow<ViewState> = discount.map {
        ViewState(
            currency = currency,
            discountAmount = it,
            discountValidationState = checkDiscountValidationState(it),
            isRemoveButtonVisible = getRemoveButtonVisibility()
        )
    }.toStateFlow(ViewState("", null))

    private fun getRemoveButtonVisibility() = with(getInitialDiscountAmount()) {
        this != null && this > BigDecimal.ZERO
    }

    @Suppress("ReturnCount")
    private fun checkDiscountValidationState(discount: BigDecimal?): DiscountAmountValidationState {
        if (discount == null) return DiscountAmountValidationState.Valid

        if (discount > orderItem.value.pricePreDiscount) {
            return DiscountAmountValidationState.Invalid(
                resourceProvider.getString(R.string.order_creation_discount_too_big_error)
            )
        }
        return DiscountAmountValidationState.Valid
    }

    fun onNavigateBack() {
        triggerEvent(MultiLiveEvent.Event.Exit)
    }

    fun onDoneClicked() {
        orderItem.updateAndGet {
            val subtotal = it.subtotal
            val total = subtotal - ((discount.value ?: BigDecimal.ZERO) * it.quantity.toBigDecimal())
            it.copy(total = total)
        }.also {
            triggerEvent(ExitWithResult(data = it))
        }
    }

    fun onDiscountRemoveClicked() {
        orderItem.updateAndGet {
            it.copy(total = it.subtotal)
        }.also {
            triggerEvent(ExitWithResult(data = it))
        }
    }

    fun onDiscountAmountChange(newDiscount: BigDecimal?) {
        discount.value = newDiscount
    }

    data class ViewState(
        val currency: String,
        val discountAmount: BigDecimal?,
        val discountValidationState: DiscountAmountValidationState = DiscountAmountValidationState.Valid,
        val isDoneButtonEnabled: Boolean = discountValidationState is DiscountAmountValidationState.Valid,
        val isRemoveButtonVisible: Boolean = false,
    )

    sealed class DiscountAmountValidationState {
        object Valid : DiscountAmountValidationState()
        data class Invalid(val errorMessage: String) : DiscountAmountValidationState()
    }

    private companion object {
        const val DEFAULT_DECIMALS_NUMBER = 2
    }
}
