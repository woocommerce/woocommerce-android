package com.woocommerce.android.ui.orders.creation.product.discount

import android.os.Parcelable
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
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
    private val currency = currencySymbolFinder.findCurrencySymbol(args.currency)
    private val orderItem: MutableStateFlow<Order.Item> =
        savedStateHandle.getStateFlow(scope = this, initialValue = args.item, key = "key_item")

    private val discount = savedStateHandle.getNullableStateFlow(
        scope = this, initialValue = getInitialDiscountAmount(), key = "key_discount", clazz = BigDecimal::class.java
    )

    private val discountType: MutableStateFlow<DiscountType> = savedStateHandle.getStateFlow(
        scope = this, initialValue = DiscountType.Amount(currency), key = "key_discount_type"
    )

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

    val viewState: StateFlow<ViewState> =
        combine(discount, discountType) { discount, type ->
            ViewState(
                currency = currency,
                discountAmount = discount,
                discountValidationState = checkDiscountValidationState(discount, type),
                isRemoveButtonVisible = getRemoveButtonVisibility(),
                discountType = type
            )
        }.toStateFlow(ViewState(currency, null))

    private fun getRemoveButtonVisibility() = with(getInitialDiscountAmount()) {
        this != null && this > BigDecimal.ZERO
    }

    private fun getInitialDiscountAmount(): BigDecimal? = with(calculateItemDiscountAmount(args.item)) {
        if (this > BigDecimal.ZERO) this else null
    }

    @Suppress("ReturnCount")
    private fun checkDiscountValidationState(discount: BigDecimal?, type: DiscountType): DiscountAmountValidationState {
        if (discount == null) return DiscountAmountValidationState.Valid

        val discountAmount: BigDecimal = when (type) {
            DiscountType.Percentage -> {
                (orderItem.value.pricePreDiscount * discount).divide(
                    PERCENTAGE_BASE,
                    PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                    RoundingMode.HALF_UP
                )
            }

            is DiscountType.Amount -> {
                discount
            }
        }
        if (discountAmount > orderItem.value.pricePreDiscount) {
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

            val total = subtotal - (getDiscountAmount() * it.quantity.toBigDecimal())
            it.copy(total = total)
        }.also {
            triggerEvent(ExitWithResult(data = it))
        }
    }

    private fun getDiscountAmount(): BigDecimal {
        val discountAmount = discount.value ?: BigDecimal.ZERO
        return when (discountType.value) {
            DiscountType.Percentage -> {
                orderItem.value.pricePreDiscount * discountAmount / PERCENTAGE_BASE
            }

            is DiscountType.Amount -> {
                discountAmount
            }
        }
    }

    fun onPercentageDiscountSelected() {
        val previousDiscountType = discountType.value
        if (previousDiscountType == DiscountType.Percentage) return

        val discountAmount = discount.value
        if (discountAmount != null) {
            val pricePreDiscount = orderItem.value.pricePreDiscount
            val discountPercentage =
                PERCENTAGE_BASE - (pricePreDiscount - discountAmount).divide(
                    pricePreDiscount,
                    PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                    RoundingMode.HALF_UP
                ) * PERCENTAGE_BASE

            discount.value = discountPercentage.stripTrailingZeros()
        }
        discountType.value = DiscountType.Percentage
    }

    fun onAmountDiscountSelected() {
        val previousDiscountType = discountType.value
        if (previousDiscountType == DiscountType.Amount(currency)) return

        val discountPercentage = discount.value
        if (discountPercentage != null) {
            val pricePreDiscount = orderItem.value.pricePreDiscount
            val discountAmount = pricePreDiscount
                .times(discountPercentage)
                .divide(PERCENTAGE_BASE, PERCENTAGE_DIVISION_QUOTIENT_SCALE, RoundingMode.HALF_UP)

            discount.value = discountAmount
                .setScale(PRICE_DIVISION_QUOTIENT_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros()
        }
        discountType.value = DiscountType.Amount(currency)
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
        val discountType: DiscountType = DiscountType.Amount(currency),
    )

    @Parcelize
    sealed class DiscountType(open val symbol: String) : Parcelable {
        @Parcelize
        object Percentage : DiscountType(PERCENTAGE_SYMBOL)

        @Parcelize
        data class Amount(override val symbol: String) : DiscountType(symbol)
    }

    sealed class DiscountAmountValidationState {
        object Valid : DiscountAmountValidationState()
        data class Invalid(val errorMessage: String) : DiscountAmountValidationState()
    }

    private companion object {
        const val PERCENTAGE_SYMBOL = "%"
        val PERCENTAGE_BASE = BigDecimal(100)
        const val PERCENTAGE_DIVISION_QUOTIENT_SCALE = 10
        const val PRICE_DIVISION_QUOTIENT_SCALE = 2
        const val DEFAULT_DECIMALS_NUMBER = 2
    }
}
