package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import javax.inject.Inject

@HiltViewModel
class OrderCreationFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_SCALE_QUOTIENT = 4
        private val PERCENTAGE_BASE = BigDecimal(100)
    }

    private val navArgs: OrderCreationFeeFragmentArgs by savedState.navArgs()
    private val orderSubtotal
        get() = navArgs.orderTotal - (navArgs.currentFeeValue ?: BigDecimal.ZERO)

    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private fun calculateFeePercentage(percentage: BigDecimal): BigDecimal {
        return orderSubtotal.multiply(percentage).divide(PERCENTAGE_BASE)
            .round(MathContext(DEFAULT_SCALE_QUOTIENT))
    }

    private fun calculatePercentageFromValue(value: BigDecimal): BigDecimal {
        if (orderSubtotal <= BigDecimal.ZERO) {
            viewState = viewState.copy(feeAmount = BigDecimal.ZERO)
            return BigDecimal.ZERO
        }

        return value.divide(orderSubtotal, DEFAULT_SCALE_QUOTIENT, RoundingMode.HALF_UP)
            .multiply(PERCENTAGE_BASE)
            .stripTrailingZeros()
    }

    init {
        viewState = viewState.copy(shouldDisplayPercentageSwitch = orderSubtotal > BigDecimal.ZERO)
        navArgs.currentFeeValue?.let { currentFee ->
            viewState = viewState.copy(
                feeAmount = currentFee,
                shouldDisplayRemoveFeeButton = true
            )
        }
    }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(viewState.feeAmount))
    }

    fun onRemoveFeeClicked() {
        triggerEvent(RemoveFee)
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        viewState = if (isChecked) {
            val feePercentage = calculatePercentageFromValue(viewState.feeAmount)
            viewState.copy(
                isPercentageSelected = isChecked,
                feePercentage = feePercentage
            )
        } else {
            viewState.copy(isPercentageSelected = isChecked)
        }
    }

    fun onFeeAmountChanged(feeAmount: BigDecimal) {
        // Only update when isPercentageSelected is not enabled
        if (viewState.isPercentageSelected) return
        viewState = viewState.copy(feeAmount = feeAmount)
    }

    fun onFeePercentageChanged(feePercentageRaw: String) {
        // Only update when isPercentageSelected is enabled
        if (!viewState.isPercentageSelected) return
        val feePercentage = feePercentageRaw.toBigDecimalOrNull() ?: BigDecimal.ZERO
        viewState = viewState.copy(
            feePercentage = feePercentage,
            feeAmount = calculateFeePercentage(feePercentage)
        )
    }

    @Parcelize
    data class ViewState(
        val feeAmount: BigDecimal = BigDecimal.ZERO,
        val feePercentage: BigDecimal = BigDecimal.ZERO,
        val isPercentageSelected: Boolean = false,
        val shouldDisplayRemoveFeeButton: Boolean = false,
        val shouldDisplayPercentageSwitch: Boolean = false
    ) : Parcelable

    data class UpdateFee(
        val amount: BigDecimal
    ) : Event()

    object RemoveFee : Event()
}
