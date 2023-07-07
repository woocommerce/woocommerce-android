package com.woocommerce.android.ui.orders.creation.product.discount

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.ResourceProvider
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.getStateFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.updateAndGet
import java.math.BigDecimal
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
class OrderCreateEditProductDiscountViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val resourceProvider: ResourceProvider,
    private val calculateItemDiscountAmount: CalculateItemDiscountAmount,
) : ScopedViewModel(savedStateHandle) {
    private val args =
        OrderCreateEditProductDiscountFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val orderItem =
        savedStateHandle.getStateFlow(scope = this, initialValue = args.item, key = "key_item")
    private val discount = savedStateHandle.getStateFlow(
        scope = this, initialValue = getInitialDiscountString(), key = "key_discount"
    )

    private val currency = Currency.getInstance(args.currency).symbol

    private fun getInitialDiscountString(): String {
        val itemDiscount = getInitialDiscountAmount()
        return if (itemDiscount > BigDecimal.ZERO) itemDiscount.toString() else ""
    }

    private fun getInitialDiscountAmount() = calculateItemDiscountAmount(args.item)

    val viewState: StateFlow<ViewState> = discount.map {
        ViewState(
            currency = currency,
            discountAmount = it,
            discountValidationState = checkDiscountValidationState(it),
            isRemoveButtonVisible = getInitialDiscountAmount() > BigDecimal.ZERO
        )
    }.toStateFlow(ViewState("", ""))

    private fun checkDiscountValidationState(discount: String): DiscountAmountValidationState {
        if (discount.isEmpty()) return DiscountAmountValidationState.Valid

        val discountAmount: BigDecimal = try {
            discount.toBigDecimal()
        } catch (e: NumberFormatException) {
            return DiscountAmountValidationState.Invalid(
                resourceProvider.getString(R.string.order_creation_discount_invalid_number_error)
            )
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
            val total = subtotal - (discount.value.toBigDecimal() * it.quantity.toBigDecimal())
            it.copy(total = total)
        }.also {
            triggerEvent(ReturnDiscountResult(item = it))
        }
    }

    fun onDiscountRemoveClicked() {
        orderItem.updateAndGet {
            it.copy(total = it.subtotal)
        }.also {
            triggerEvent(ReturnDiscountResult(item = it))
        }
    }

    fun onDiscountAmountChange(newDiscount: String) {
        discount.update { _ ->
            newDiscount
        }
    }

    data class ViewState(
        val currency: String,
        val discountAmount: String,
        val discountValidationState: DiscountAmountValidationState = DiscountAmountValidationState.Valid,
        val isDoneButtonEnabled: Boolean = discountValidationState is DiscountAmountValidationState.Valid,
        val isRemoveButtonVisible: Boolean = false,
    )

    sealed class DiscountAmountValidationState {
        object Valid : DiscountAmountValidationState()
        data class Invalid(val errorMessage: String) : DiscountAmountValidationState()
    }

    data class ReturnDiscountResult(val item: Order.Item) : MultiLiveEvent.Event()
}

