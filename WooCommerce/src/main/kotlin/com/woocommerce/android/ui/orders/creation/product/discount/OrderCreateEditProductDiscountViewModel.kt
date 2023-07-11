package com.woocommerce.android.ui.orders.creation.product.discount

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditProductDiscountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val calculateItemDiscountAmount: CalculateItemDiscountAmount,
    currencySymbolFinder: CurrencySymbolFinder,
) : ScopedViewModel(savedStateHandle) {
    private val args =
        OrderCreateEditProductDiscountFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val currency = currencySymbolFinder.findCurrencySymbol(args.currency)

    private val orderItem =
        savedStateHandle.getStateFlow(scope = this, initialValue = args.item, key = "key_item")

    private val discountAmountText = savedStateHandle.getStateFlow(
        scope = this, initialValue = getInitialDiscountString(), key = "key_discount_text"
    )

    private val discountType: MutableStateFlow<DiscountType> = savedStateHandle.getStateFlow(
        scope = this, initialValue = DiscountType.Amount(currency), key = "key_discount_type"
    )

    private fun getInitialDiscountString(): String {
        val itemDiscount = getInitialDiscountAmount()
        return if (itemDiscount > BigDecimal.ZERO) itemDiscount.toString() else ""
    }

    private fun getInitialDiscountAmount() = calculateItemDiscountAmount(args.item)

    val viewState: StateFlow<ViewState> = combine(discountAmountText, discountType) { discount, type ->
        ViewState(
            currency = currency,
            discountAmount = discount,
            discountValidationState = checkDiscountValidationState(discount),
            isRemoveButtonVisible = getInitialDiscountAmount() > BigDecimal.ZERO,
            discountType = type
        )
    }.toStateFlow(ViewState("", ""))

    @Suppress("ReturnCount")
    private fun checkDiscountValidationState(discount: String): DiscountAmountValidationState {
        if (discount.isEmpty()) return DiscountAmountValidationState.Valid

        val discountAmount: BigDecimal = try {
            discount.toBigDecimal()
        } catch (e: NumberFormatException) {
            return DiscountAmountValidationState.Invalid(
                resourceProvider.getString(R.string.order_creation_discount_invalid_number_error)
            )
        }.run {
            when (discountType.value) {
                DiscountType.Percentage -> {
                    (orderItem.value.pricePreDiscount * this).divide(
                        PERCENTAGE_BASE,
                        PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                        RoundingMode.HALF_UP
                    )
                }
                is DiscountType.Amount -> {
                    this
                }
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
        val discountAmount = discountAmountText.value.toBigDecimal()
        return when (discountType.value) {
            DiscountType.Percentage -> {
                orderItem.value.pricePreDiscount * discountAmount / 100.toBigDecimal()
            }
            is DiscountType.Amount -> {
                discountAmount
            }
        }
    }

    fun onPercentageDiscountSelected() {
        val previousDiscountType = discountType.value
        if (previousDiscountType == DiscountType.Percentage) return

        val discountAmount = discountAmountText.value.toBigDecimal().setScale(2, RoundingMode.HALF_UP)

        val pricePreDiscount = orderItem.value.pricePreDiscount
        val discountPercentage =
            PERCENTAGE_BASE - (pricePreDiscount - discountAmount).divide(
                pricePreDiscount,
                PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                RoundingMode.HALF_UP
            ) * PERCENTAGE_BASE

        discountAmountText.value = discountPercentage.stripTrailingZeros().toPlainString()

        discountType.value = DiscountType.Percentage
    }

    fun onAmountDiscountSelected() {
        val previousDiscountType = discountType.value
        if (previousDiscountType == DiscountType.Amount(currency)) return

        val discountPercentage = discountAmountText.value.toBigDecimal()
        val pricePreDiscount = orderItem.value.pricePreDiscount
        val discountAmount =
            pricePreDiscount *
                discountPercentage.divide(
                    PERCENTAGE_BASE,
                    PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                    RoundingMode.HALF_UP
                )

        discountAmountText.value =
            discountAmount.setScale(PRICE_DIVISION_QUOTIENT_SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString()

        discountType.value = DiscountType.Amount(currency)
    }

    fun onDiscountRemoveClicked() {
        orderItem.updateAndGet {
            it.copy(total = it.subtotal)
        }.also {
            triggerEvent(ExitWithResult(data = it))
        }
    }

    fun onDiscountAmountChange(newDiscount: String) {
        discountAmountText.value = newDiscount
    }

    data class ViewState(
        val currency: String,
        val discountAmount: String,
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
    }
}
