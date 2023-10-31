package com.woocommerce.android.ui.orders.creation.product.discount

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_REMOVE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_ORDER_DISCOUNT_TYPE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORDER_DISCOUNT_TYPE_FIXED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_ORDER_DISCOUNT_TYPE_PERCENTAGE
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.orders.creation.OrderCreationProduct
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
    private val tracker: AnalyticsTrackerWrapper,
    siteParamsRepo: ParameterRepository,
    currencySymbolFinder: CurrencySymbolFinder,
) : ScopedViewModel(savedStateHandle) {
    private val args =
        OrderCreateEditProductDiscountFragmentArgs.fromSavedStateHandle(savedStateHandle)
    private val currency = currencySymbolFinder.findCurrencySymbol(args.currency)
    val productItem: MutableStateFlow<OrderCreationProduct> =
        savedStateHandle.getStateFlow(
            scope = this,
            initialValue = args.item.copyProduct(item = args.item.item.copy(total = args.item.item.pricePreDiscount)),
            key = "key_item"
        )

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
                discountType = type,
                priceAfterDiscount = getPriceAfterDiscount(),
                calculatedPriceAfterDiscount = getCalculatedPriceAfterDiscount(),
                productDetailsState = ProductDetailsState(
                    imageUrl = productItem.value.productInfo.imageUrl
                )
            )
        }.toStateFlow(ViewState(currency, null))

    private fun getRemoveButtonVisibility() = with(getInitialDiscountAmount()) {
        this != null && this > BigDecimal.ZERO
    }

    private fun getInitialDiscountAmount(): BigDecimal? = with(calculateItemDiscountAmount(args.item.item)) {
        if (this > BigDecimal.ZERO) this else null
    }

    @Suppress("ReturnCount")
    private fun checkDiscountValidationState(discount: BigDecimal?, type: DiscountType): DiscountAmountValidationState {
        if (discount == null) return DiscountAmountValidationState.Valid

        val discountAmount: BigDecimal = when (type) {
            DiscountType.Percentage -> {
                (productItem.value.item.pricePreDiscount * discount).divide(
                    PERCENTAGE_BASE,
                    PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                    RoundingMode.HALF_UP
                )
            }

            is DiscountType.Amount -> {
                discount
            }
        }
        if (
            discountAmount > (productItem.value.item.pricePreDiscount * productItem.value.item.quantity.toBigDecimal())
        ) {
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
        productItem.updateAndGet {
            val subtotal = it.item.subtotal

            val total = subtotal - getDiscountAmount()
            it.copyProduct(it.item.copy(total = total))
        }.also {
            triggerEvent(ExitWithResult(data = it))
            tracker.track(
                AnalyticsEvent.ORDER_PRODUCT_DISCOUNT_ADD,
                mapOf(
                    KEY_ORDER_DISCOUNT_TYPE to when (discountType.value) {
                        DiscountType.Percentage -> VALUE_ORDER_DISCOUNT_TYPE_PERCENTAGE
                        is DiscountType.Amount -> VALUE_ORDER_DISCOUNT_TYPE_FIXED
                    }
                )
            )
        }
    }

    private fun getDiscountAmount(): BigDecimal {
        val discountAmount = discount.value ?: BigDecimal.ZERO
        return when (discountType.value) {
            DiscountType.Percentage -> {
                productItem.value.item.subtotal * discountAmount / PERCENTAGE_BASE
            }

            is DiscountType.Amount -> {
                discountAmount
            }
        }
    }

    fun onPercentageDiscountSelected() {
        if (discountType.value == DiscountType.Percentage) return

        discount.value = discount.value?.let { calculateDiscountPercentage(it) }
        discountType.value = DiscountType.Percentage
    }

    fun onAmountDiscountSelected() {
        if (discountType.value == DiscountType.Amount(currency)) return

        discount.value = discount.value?.let { calculateDiscountAmount(it) }
        discountType.value = DiscountType.Amount(currency)
    }

    private fun calculateDiscountPercentage(discountAmount: BigDecimal): BigDecimal {
        val pricePreDiscount = productItem.value.item.pricePreDiscount * productItem.value.item.quantity.toBigDecimal()
        val discountPercentage = if (pricePreDiscount > BigDecimal.ZERO) {
            PERCENTAGE_BASE - (pricePreDiscount - discountAmount).divide(
                pricePreDiscount,
                PERCENTAGE_DIVISION_QUOTIENT_SCALE,
                RoundingMode.HALF_UP
            ) * PERCENTAGE_BASE
        } else {
            BigDecimal.ZERO
        }
        return discountPercentage.stripTrailingZeros()
    }

    private fun calculateDiscountAmount(discountPercentage: BigDecimal): BigDecimal {
        val pricePreDiscount = productItem.value.item.pricePreDiscount
        val discountAmount = pricePreDiscount
            .times(discountPercentage)
            .divide(PERCENTAGE_BASE, PERCENTAGE_DIVISION_QUOTIENT_SCALE, RoundingMode.HALF_UP)

        return (discountAmount * productItem.value.item.quantity.toBigDecimal())
            .setScale(2, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    }

    fun onDiscountRemoveClicked() {
        productItem.updateAndGet {
            it.copyProduct(it.item.copy(total = it.item.subtotal))
        }.also {
            triggerEvent(ExitWithResult(data = it))
            tracker.track(ORDER_PRODUCT_DISCOUNT_REMOVE)
        }
    }

    private fun getPriceAfterDiscount(): BigDecimal {
        return if (discount.value == null) BigDecimal.ZERO else productItem.value.item.subtotal - getDiscountAmount()
            .setScale(2, RoundingMode.HALF_UP)
    }

    private fun getCalculatedPriceAfterDiscount(): BigDecimal {
        return if (discount.value == null) BigDecimal.ZERO else if (discountType.value == DiscountType.Percentage) {
            discount.value?.let {
                calculateDiscountAmount(it)
                    .setScale(2, RoundingMode.HALF_UP)
            } ?: BigDecimal.ZERO
        } else {
            discount.value?.let {
                calculateDiscountPercentage(it)
                    .setScale(2, RoundingMode.HALF_UP)
            } ?: BigDecimal.ZERO
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
        val priceAfterDiscount: BigDecimal = BigDecimal.ZERO,
        val calculatedPriceAfterDiscount: BigDecimal = BigDecimal.ZERO,
        val productDetailsState: ProductDetailsState? = null,
    )

    data class ProductDetailsState(
        val imageUrl: String,
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
