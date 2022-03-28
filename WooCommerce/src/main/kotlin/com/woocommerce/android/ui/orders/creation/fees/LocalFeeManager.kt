package com.woocommerce.android.ui.orders.creation.fees

import com.woocommerce.android.extensions.exhaustive
import com.woocommerce.android.model.Order
import java.math.BigDecimal

class LocalFeeManager(private val feeName: String) {
    var localFeeLine: LocalFeeLine = LocalFeeLine.EMPTY
        private set

    fun updateFeeLine(feeLines: List<Order.FeeLine>, orderTotal: BigDecimal) {
        if (feeLines.isEmpty()) {
            localFeeLine = LocalFeeLine.EMPTY
            return
        }
        val orderFeeLine = feeLines.first()
        when (localFeeLine.getType()) {
            LocalFeeLineType.PERCENTAGE -> {
                if (orderTotal.compareTo(BigDecimal.ZERO) == 0) {
                    onFeeRemoved()
                    return
                }
                localFeeLine = (localFeeLine as LocalPercentageFee).copy(
                    id = orderFeeLine.id,
                    name = orderFeeLine.name,
                    totalTax = orderFeeLine.totalTax,
                    orderTotal = orderTotal
                )
            }
            LocalFeeLineType.AMOUNT -> {
                localFeeLine = (localFeeLine as LocalAmountFee).copy(
                    id = orderFeeLine.id,
                    name = orderFeeLine.name,
                    totalTax = orderFeeLine.totalTax
                )
            }
        }.exhaustive
    }

    fun editFee(editedLocalFee: LocalFeeLine) {
        localFeeLine =
            if (editedLocalFee.name.isNullOrEmpty())
                editedLocalFee.copyFee(name = feeName)
            else
                editedLocalFee
    }

    fun getFeeLines(): List<Order.FeeLine> = if (localFeeLine == LocalFeeLine.EMPTY) {
        emptyList()
    } else {
        listOf(localFeeLine.toOrderFeeLine())
    }

    fun onFeeRemoved() {
        localFeeLine = LocalFeeLine.EMPTY.copy(id = localFeeLine.id, name = null)
    }
}
