package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.AMOUNT
import com.woocommerce.android.ui.orders.creation.fees.OrderCreationAddFeeViewModel.FeeType.PERCENTAGE
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.ScopedViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class OrderCreationAddFeeViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    val viewStateData = LiveDataDelegate(savedState, ViewState())
    private var viewState by viewStateData

    fun onDoneSelected(isPercentageSelected: Boolean) {
        // TODO: handle possible exception from parsing string to bigDecimal
        viewState.inputValue?.let {
            triggerEvent(
                FeeCreationData(it.toBigDecimal(), feeTypeWhen(isPercentageSelected))
            )
        }
    }

    private fun feeTypeWhen(isPercentageSelected: Boolean) =
        when (isPercentageSelected) {
            true -> PERCENTAGE
            false -> AMOUNT
        }

    @Parcelize
    data class ViewState(
        val inputValue: String? = null
    ) : Parcelable

    enum class FeeType {
        AMOUNT, PERCENTAGE
    }

    data class FeeCreationData(
        val amount: BigDecimal,
        val feeType: FeeType
    ) : Event()
}
