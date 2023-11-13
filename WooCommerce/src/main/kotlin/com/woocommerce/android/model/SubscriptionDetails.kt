package com.woocommerce.android.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class SubscriptionDetails(
    val price: BigDecimal,
    val period: SubscriptionPeriod,
    val periodInterval: Int,
    val length: Int?,
    val signUpFee: BigDecimal?,
    val trialPeriod: SubscriptionPeriod?,
    val trialLength: Int?,
    val oneTimeShipping: Boolean
) : Parcelable
