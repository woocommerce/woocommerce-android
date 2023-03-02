package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal
import java.util.Date

@Parcelize
data class Subscription(
    val id: Long,
    val status: Status,
    val billingPeriod: Period,
    val billingInterval: Int,
    val total: BigDecimal,
    val startDate: Date,
    val endDate: Date?,
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
    @Parcelize
    sealed class Period(val value: String) : Parcelable {
        companion object {
            fun fromValue(value: String): Period {
                return when (value) {
                    Day.value -> Day
                    Week.value -> Week
                    Month.value -> Month
                    Year.value -> Year
                    else -> Custom(value)
                }
            }
        }
        @Parcelize
        object Day : Period("day")
        @Parcelize
        object Week : Period("week")
        @Parcelize
        object Month : Period("month")
        @Parcelize
        object Year : Period("year")
        @Parcelize
        data class Custom(private val customValue: String) : Period(customValue)
    }
}
