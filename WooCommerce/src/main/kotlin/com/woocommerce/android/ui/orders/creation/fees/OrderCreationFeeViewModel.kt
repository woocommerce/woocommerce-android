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
import java.math.RoundingMode.HALF_UP
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

    private val activeFeeValue
        get() = when (viewState.isPercentageSelected) {
            true -> ((orderSubtotal * viewState.feePercentage) / PERCENTAGE_BASE)
                .round(MathContext(DEFAULT_SCALE_QUOTIENT))
            false -> viewState.feeAmount
        }

    private val appliedPercentageFromCurrentFeeValue
        get() = navArgs.currentFeeValue
            ?.takeIf { orderSubtotal > BigDecimal.ZERO }
            ?.let { it.divide(orderSubtotal, DEFAULT_SCALE_QUOTIENT, HALF_UP) * PERCENTAGE_BASE }
            ?.stripTrailingZeros()
            ?: BigDecimal.ZERO

    init {
        viewState = viewState.copy(shouldDisplayPercentageSwitch = orderSubtotal > BigDecimal.ZERO)
        navArgs.currentFeeValue?.let {
            viewState = viewState.copy(
                feeAmount = it,
                feePercentage = appliedPercentageFromCurrentFeeValue,
                shouldDisplayRemoveFeeButton = true
            )
        }
    }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(activeFeeValue))
    }

    fun onRemoveFeeClicked() {
        triggerEvent(RemoveFee)
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        viewState = viewState.copy(isPercentageSelected = isChecked)
    }

    fun onFeeAmountChanged(feeAmount: BigDecimal) {
        viewState = viewState.copy(feeAmount = feeAmount)
    }

    fun onFeePercentageChanged(feePercentage: String) {
        viewState = viewState.copy(
            feePercentage = feePercentage.toBigDecimalOrNull() ?: BigDecimal.ZERO
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
