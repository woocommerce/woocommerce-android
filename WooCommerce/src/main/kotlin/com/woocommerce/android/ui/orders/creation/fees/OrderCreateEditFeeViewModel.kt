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
class OrderCreateEditFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    companion object {
        private const val DEFAULT_SCALE_QUOTIENT = 4
        private val PERCENTAGE_BASE = BigDecimal(100)
    }

    private val navArgs: OrderCreateEditFeeFragmentArgs by savedState.navArgs()
    private val orderSubtotal = navArgs.orderSubTotal

    /**
     * Saving more than necessary into the SavedState has associated risks which were not known at the time this
     * field was implemented - after we ensure we don't save unnecessary data, we can
     * replace @Suppress("OPT_IN_USAGE") with @OptIn(LiveDelegateSavedStateAPI::class).
     */
    @Suppress("OPT_IN_USAGE")
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private fun calculateFeePercentage(percentage: BigDecimal): BigDecimal {
        return orderSubtotal.multiply(percentage).divide(PERCENTAGE_BASE)
            .round(MathContext(DEFAULT_SCALE_QUOTIENT))
    }

    private fun calculatePercentageFromValue(value: BigDecimal): BigDecimal {
        if (orderSubtotal <= BigDecimal.ZERO) return BigDecimal.ZERO

        return value.divide(orderSubtotal, DEFAULT_SCALE_QUOTIENT, RoundingMode.HALF_UP)
            .multiply(PERCENTAGE_BASE)
            .stripTrailingZeros()
    }

    init {
        navArgs.currentFeeValue?.let { currentFee ->
            viewState = viewState.copy(
                shouldDisplayPercentageSwitch = false,
                feeAmount = currentFee,
                feePercentage = calculatePercentageFromValue(currentFee),
                isDoneButtonEnabled = shouldEnableDoneButtonForAmount(currentFee),
                shouldDisplayRemoveFeeButton = true
            )
        } ?: run {
            viewState = viewState.copy(shouldDisplayPercentageSwitch = orderSubtotal > BigDecimal.ZERO)
        }
    }

    fun onDoneSelected() {
        val feeAmount = if (viewState.isPercentageSelected) {
            calculateFeePercentage(viewState.feePercentage)
        } else {
            viewState.feeAmount
        }
        triggerEvent(UpdateFee(feeAmount))
    }

    fun onRemoveFeeClicked() {
        triggerEvent(RemoveFee)
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        viewState = viewState.copy(isPercentageSelected = isChecked)
    }

    fun onFeeAmountChanged(feeAmount: BigDecimal) {
        // Only update when isPercentageSelected is not enabled
        if (viewState.isPercentageSelected) return
        viewState = viewState.copy(
            feeAmount = feeAmount,
            feePercentage = calculatePercentageFromValue(feeAmount),
            isDoneButtonEnabled = shouldEnableDoneButtonForAmount(feeAmount)
        )
    }

    fun onFeePercentageChanged(feePercentageRaw: String) {
        // Only update when isPercentageSelected is enabled
        if (!viewState.isPercentageSelected) return
        val feePercentage = feePercentageRaw.toBigDecimalOrNull() ?: BigDecimal.ZERO
        val feeAmount = calculateFeePercentage(feePercentage)
        viewState = viewState.copy(
            feePercentage = feePercentage,
            feeAmount = feeAmount,
            isDoneButtonEnabled = shouldEnableDoneButtonForAmount(feeAmount)
        )
    }

    private fun shouldEnableDoneButtonForAmount(amount: BigDecimal) = amount != BigDecimal.ZERO

    @Parcelize
    data class ViewState(
        val feeAmount: BigDecimal = BigDecimal.ZERO,
        val feePercentage: BigDecimal = BigDecimal.ZERO,
        val isPercentageSelected: Boolean = false,
        val isDoneButtonEnabled: Boolean = false,
        val shouldDisplayRemoveFeeButton: Boolean = false,
        val shouldDisplayPercentageSwitch: Boolean = false
    ) : Parcelable

    data class UpdateFee(
        val amount: BigDecimal
    ) : Event()

    object RemoveFee : Event()
}
