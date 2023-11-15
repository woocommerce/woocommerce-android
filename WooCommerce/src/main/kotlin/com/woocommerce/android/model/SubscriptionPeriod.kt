package com.woocommerce.android.model

import android.content.Context
import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.Parcelize

@Parcelize
sealed class SubscriptionPeriod(val value: String) : Parcelable {
    companion object {
        fun fromValue(value: String): SubscriptionPeriod {
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
    object Day : SubscriptionPeriod("day")
    @Parcelize
    object Week : SubscriptionPeriod("week")
    @Parcelize
    object Month : SubscriptionPeriod("month")
    @Parcelize
    object Year : SubscriptionPeriod("year")
    @Parcelize
    data class Custom(private val customValue: String) : SubscriptionPeriod(customValue)

    fun getPeriodString(
        context: Context,
        billingInterval: Int
    ): String {
        return when (this) {
            Day -> StringUtils.getQuantityString(
                context = context,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_days,
                one = R.string.subscription_period_day
            )
            Week -> StringUtils.getQuantityString(
                context = context,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_weeks,
                one = R.string.subscription_period_week
            )
            Month -> StringUtils.getQuantityString(
                context = context,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_months,
                one = R.string.subscription_period_month
            )
            Year -> StringUtils.getQuantityString(
                context = context,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_years,
                one = R.string.subscription_period_year
            )
            is Custom -> this.value
        }
    }

    fun getPeriodString(
        resources: ResourceProvider,
        billingInterval: Int
    ): String {
        return when (this) {
            Day -> StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_days,
                one = R.string.subscription_period_day
            )
            Week -> StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_weeks,
                one = R.string.subscription_period_week
            )
            Month -> StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_months,
                one = R.string.subscription_period_month
            )

            Year -> StringUtils.getQuantityString(
                resourceProvider = resources,
                quantity = billingInterval,
                default = R.string.subscription_period_multiple_years,
                one = R.string.subscription_period_year
            )

            is Custom -> this.value
        }
    }

    @Suppress("MagicNumber")
    fun getRangeForPeriod(): IntRange {
        return when (this) {
            Day -> 1..90
            Week -> 1..52
            Month -> 1..24
            Year -> 1..5
            is Custom -> 0..1
        }
    }
}
