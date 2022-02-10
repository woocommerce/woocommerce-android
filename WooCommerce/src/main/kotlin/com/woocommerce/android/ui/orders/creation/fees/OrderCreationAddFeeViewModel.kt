package com.woocommerce.android.ui.orders.creation.fees

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.AMOUNT
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.PERCENTAGE
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationAddFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    fun onDoneSelected(input: String, isPercentageSelected: Boolean) {
        // TODO: handle possible exception from parsing string to bigDecimal
        triggerEvent(
            FeeCreationData(input.toBigDecimal(), feeTypeWhen(isPercentageSelected))
        )
    }

    fun feeTypeWhen(isPercentageSelected: Boolean) =
        when (isPercentageSelected) {
            true -> PERCENTAGE
            false -> AMOUNT
        }

    enum class FeeType {
        AMOUNT, PERCENTAGE
    }

    data class FeeCreationData(
        val amount: BigDecimal,
        val feeType: FeeType
    ) : Event()
}
