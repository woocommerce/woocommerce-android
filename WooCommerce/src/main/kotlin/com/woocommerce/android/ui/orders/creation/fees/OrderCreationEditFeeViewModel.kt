package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationEditFeeViewModel.FeeType.AMOUNT
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationEditFeeViewModel.FeeType.PERCENTAGE
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationEditFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    private val activeFeeType
        get() = when (viewState.isPercentageSelected) {
            true -> PERCENTAGE
            false -> AMOUNT
        }

    private val activeFeeValue
        get() = when (viewState.isPercentageSelected) {
            true -> viewState.feePercentage
            false -> viewState.feeAmount
        }

    fun onDoneSelected() {
        triggerEvent(UpdateFee(activeFeeValue, activeFeeType))
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
        val isPercentageSelected: Boolean = false
    ) : Parcelable

    enum class FeeType {
        AMOUNT, PERCENTAGE
    }
    data class UpdateFee(
        val amount: BigDecimal,
        val feeType: FeeType
    ) : Event()
}
