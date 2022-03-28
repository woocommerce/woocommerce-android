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
import javax.inject.Inject

@HiltViewModel
class OrderCreationFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    private val navArgs: OrderCreationFeeFragmentArgs by savedState.navArgs()

    val orderTotal = navArgs.orderTotal
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private var activeLocalFee: LocalFeeLine = navArgs.currentFee ?: LocalFeeLine.EMPTY

    init {
        val isPercentageFee = activeLocalFee.getType() == LocalFeeLineType.PERCENTAGE
        viewState = viewState.copy(
            feeAmount = activeLocalFee.getTotal(),
            feePercentage = if (isPercentageFee) activeLocalFee.amount else BigDecimal.ZERO,
            isPercentageSelected = isPercentageFee,
            shouldDisplayRemoveFeeButton = activeLocalFee.id != 0L,
            shouldDisplayPercentageSwitch = orderTotal > BigDecimal.ZERO
        )
    }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(activeLocalFee))
    }

    fun onRemoveFeeClicked() {
        triggerEvent(RemoveFee)
    }

    fun onPercentageSwitchChanged(isChecked: Boolean) {
        activeLocalFee = if (isChecked) {
            LocalPercentageFee.toPercentageFee(activeLocalFee, orderTotal)
        } else {
            LocalAmountFee.toAmountFee(activeLocalFee)
        }
        viewState = viewState.copy(isPercentageSelected = isChecked)
    }

    fun onFeeAmountChanged(feeAmount: BigDecimal) {
        viewState = viewState.copy(feeAmount = feeAmount)
        activeLocalFee = activeLocalFee.copyFee(amount = feeAmount)
    }

    fun onFeePercentageChanged(feePercentageRaw: String) {
        val feePercentage = feePercentageRaw.toBigDecimalOrNull() ?: BigDecimal.ZERO
        viewState = viewState.copy(feePercentage = feePercentage)
        activeLocalFee = activeLocalFee.copyFee(amount = feePercentage)
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
        val amount: LocalFeeLine
    ) : Event()

    object RemoveFee : Event()
}
