package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.time.LocalDate

@Parcelize
data class Subscription(
    val id: Long,
    val status: Status,
    val billingPeriod: SubscriptionPeriod,
    val billingInterval: Int,
    val total: BigDecimal,
    val startDate: LocalDate,
    val endDate: LocalDate?,
    val currency: String
) : Parcelable {
    @Parcelize
    sealed class Status(val value: String) : Parcelable {
        companion object {
            fun fromValue(value: String): Status {
                return when (value) {
                    Active.value -> Active
                    OnHold.value -> OnHold
                    Cancelled.value -> Cancelled
                    Expired.value -> Expired
                    PendingCancellation.value -> PendingCancellation
                    else -> Custom(value)
                }
            }
        }

        @Parcelize
        object Active : Status("active")

        @Parcelize
        object OnHold : Status("on-hold")

        @Parcelize
        object Cancelled : Status("cancelled")

        @Parcelize
        object Expired : Status("expired")

        @Parcelize
        object PendingCancellation : Status("pending_cancellation")

        @Parcelize
        data class Custom(private val customValue: String) : Status(customValue)
    }
}
