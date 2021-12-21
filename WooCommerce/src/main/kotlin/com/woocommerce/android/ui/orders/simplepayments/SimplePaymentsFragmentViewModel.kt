package com.woocommerce.android.ui.orders.simplepayments

import android.os.Parcelable
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.annotations.OpenClassOnDebug
import com.woocommerce.android.model.Order
import com.woocommerce.android.viewmodel.LiveDataDelegate
import com.woocommerce.android.viewmodel.ScopedViewModel
import com.woocommerce.android.viewmodel.navArgs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import javax.inject.Inject

@OpenClassOnDebug
@HiltViewModel
class SimplePaymentsFragmentViewModel @Inject constructor(
    savedState: SavedStateHandle
) : ScopedViewModel(savedState) {
    final val viewStateLiveData = LiveDataDelegate(savedState, ViewState())
    internal final var viewState by viewStateLiveData

    private val navArgs: SimplePaymentsFragmentArgs by savedState.navArgs()

    private val order: Order
        get() = navArgs.order

    init {
        val hasTaxes = order.totalTax > BigDecimal.ZERO
        updateViewState(hasTaxes)
    }

    private fun updateViewState(chargeTaxes: Boolean) {
        val feeLine = order.feesLines[0]
        if (chargeTaxes) {
            viewState = viewState.copy(
                chargeTaxes = true,
                orderTotalTax = order.totalTax,
                orderTotal = order.total,
                orderSubtotal = feeLine.total
            )
        } else {
            viewState = viewState.copy(
                chargeTaxes = false,
                orderTotalTax = BigDecimal.ZERO,
                orderTotal = feeLine.total,
                orderSubtotal = feeLine.total
            )
        }
    }

    fun onChargeTaxesChanged(chargeTaxes: Boolean) {
        updateViewState(chargeTaxes = chargeTaxes)
    }

    @Parcelize
    data class ViewState(
        val chargeTaxes: Boolean = false,
        val orderSubtotal: BigDecimal = BigDecimal.ZERO,
        val orderTotalTax: BigDecimal = BigDecimal.ZERO,
        val orderTotal: BigDecimal = BigDecimal.ZERO,
    ) : Parcelable
}
