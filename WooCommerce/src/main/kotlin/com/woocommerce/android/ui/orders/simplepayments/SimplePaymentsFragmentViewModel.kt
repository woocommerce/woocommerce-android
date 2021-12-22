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
        viewState = viewState.copy(customerNote = order.customerNote)
        val hasTaxes = order.totalTax > BigDecimal.ZERO
        updateViewState(hasTaxes)
    }

    private fun updateViewState(chargeTaxes: Boolean) {
        // accessing feesLines[0] is safe to do since a fee line is passed by FluxC when creating the order. also note
        // the single fee line is the only way to get the price w/o taxes, and FluxC sets the tax status to "taxable"
        // so when the order is created core automatically sets the total tax if the store has taxes enabled.
        val feeLine = order.feesLines[0]

        if (chargeTaxes) {
            val taxPercent = if (feeLine.total > BigDecimal.ZERO) {
                (order.totalTax.toFloat() / feeLine.total.toFloat()) * ONE_HUNDRED
            } else {
                0f
            }
            viewState = viewState.copy(
                chargeTaxes = true,
                orderSubtotal = feeLine.total,
                orderTotalTax = order.totalTax,
                orderTaxPercent = taxPercent,
                orderTotal = order.total
            )
        } else {
            viewState = viewState.copy(
                chargeTaxes = false,
                orderSubtotal = feeLine.total,
                orderTotalTax = BigDecimal.ZERO,
                orderTaxPercent = 0f,
                orderTotal = feeLine.total
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
        val orderTaxPercent: Float = 0f,
        val orderTotal: BigDecimal = BigDecimal.ZERO,
        val customerNote: String = ""
    ) : Parcelable

    companion object {
        private const val ONE_HUNDRED = 100f
    }
}
