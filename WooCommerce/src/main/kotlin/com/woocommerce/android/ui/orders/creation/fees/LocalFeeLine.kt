package com.woocommerce.android.ui.orders.creation.fees

import android.os.Parcelable
import com.woocommerce.android.model.Order
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

enum class LocalFeeLineType { AMOUNT, PERCENTAGE }

interface LocalFeeLine : Parcelable {
    val id: Long
    val name: String?
    val amount: BigDecimal
    val totalTax: BigDecimal

    fun toOrderFeeLine(
        id: Long = this.id,
        name: String? = this.name,
        totalTax: BigDecimal = this.totalTax
    ): Order.FeeLine

    fun getTotal(): BigDecimal
    fun getType(): LocalFeeLineType

    fun copyFee(
        id: Long = this.id,
        name: String? = this.name,
        amount: BigDecimal = this.amount,
        totalTax: BigDecimal = this.totalTax
    ): LocalFeeLine

    companion object {
        val EMPTY = LocalAmountFee(
            id = 0,
            name = "",
            amount = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO
        )
    }
}

@Parcelize
data class LocalAmountFee(
    override val id: Long,
    override val name: String?,
    override var amount: BigDecimal,
    override val totalTax: BigDecimal,
) : LocalFeeLine, Parcelable {
    override fun getTotal(): BigDecimal = amount
    override fun getType(): LocalFeeLineType = LocalFeeLineType.AMOUNT
    override fun copyFee(
        id: Long,
        name: String?,
        amount: BigDecimal,
        totalTax: BigDecimal
    ): LocalFeeLine = LocalAmountFee(
        id = id,
        name = name,
        amount = amount,
        totalTax = totalTax
    )

    override fun toOrderFeeLine(
        id: Long,
        name: String?,
        totalTax: BigDecimal
    ): Order.FeeLine =
        Order.FeeLine(
            id = id,
            name = name,
            total = getTotal(),
            totalTax = totalTax
        )

    companion object {
        fun toAmountFee(localFeeLine: LocalFeeLine): LocalAmountFee {
            return LocalAmountFee(
                id = localFeeLine.id,
                amount = localFeeLine.amount,
                name = localFeeLine.name,
                totalTax = localFeeLine.totalTax
            )
        }
    }
}

@Parcelize
data class LocalPercentageFee(
    override val id: Long,
    override val name: String?,
    override var amount: BigDecimal,
    override val totalTax: BigDecimal,
    val orderTotal: BigDecimal
) : LocalFeeLine, Parcelable {
    override fun getTotal(): BigDecimal = (amount * orderTotal) / PERCENTAGE_BASE
    override fun getType(): LocalFeeLineType = LocalFeeLineType.PERCENTAGE
    override fun copyFee(
        id: Long,
        name: String?,
        amount: BigDecimal,
        totalTax: BigDecimal
    ): LocalFeeLine = LocalPercentageFee(
        id = id,
        name = name,
        amount = amount,
        totalTax = totalTax,
        orderTotal = orderTotal
    )

    override fun toOrderFeeLine(
        id: Long,
        name: String?,
        totalTax: BigDecimal
    ): Order.FeeLine =
        Order.FeeLine(
            id = id,
            name = name,
            total = getTotal(),
            totalTax = totalTax
        )

    companion object {
        private val PERCENTAGE_BASE = BigDecimal(100)
        fun toPercentageFee(localFeeLine: LocalFeeLine, orderTotal: BigDecimal): LocalPercentageFee {
            return LocalPercentageFee(
                id = localFeeLine.id,
                amount = localFeeLine.amount,
                name = localFeeLine.name,
                totalTax = localFeeLine.totalTax,
                orderTotal = orderTotal
            )
        }
    }
}
