package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed interface SubscriptionPaymentSyncDate : Parcelable {
    @Parcelize
    object None : SubscriptionPaymentSyncDate

    @Parcelize
    @JvmInline
    value class Day(val value: Int) : SubscriptionPaymentSyncDate

    @Parcelize
    data class MonthDay(val month: Int, val day: Int) : SubscriptionPaymentSyncDate
}
