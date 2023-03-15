package com.woocommerce.android.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.math.BigDecimal

object SubscriptionDetailsMapper {
    private val gson by lazy { Gson() }
    fun toAppModel(metadata: String): SubscriptionDetails? {
        val jsonArray = gson.fromJson(metadata, JsonArray::class.java)

        val subscriptionInformation = jsonArray
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject -> jsonObject[MetadataKeys.KEY].asString in SubscriptionDetailsKeys.Keys }
            .associate { jsonObject ->
                jsonObject[MetadataKeys.KEY].asString to jsonObject[MetadataKeys.VALUE].asString
            }

        return if (subscriptionInformation.isNotEmpty()) {
            val price = subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_PRICE]?.toBigDecimalOrNull()
                ?: BigDecimal.ZERO

            val periodString = subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_PERIOD] ?: ""
            val period = SubscriptionPeriod.fromValue(periodString)

            val periodIntervalString =
                subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_PERIOD_INTERVAL] ?: ""
            val periodInterval = periodIntervalString.toIntOrNull() ?: 0

            val lengthString = subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_LENGTH] ?: ""
            val lengthInt = lengthString.toIntOrNull()
            val length = if (lengthInt != null && lengthInt > 0) lengthInt else null

            val signUpFee =
                subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_SIGN_UP_FEE]?.toBigDecimalOrNull()

            val trialPeriodString = subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_TRIAL_PERIOD]
            val trialPeriod = trialPeriodString?.let { SubscriptionPeriod.fromValue(trialPeriodString) }

            val trialLengthString = subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_TRIAL_LENGTH] ?: ""
            val trialLengthInt = trialLengthString.toIntOrNull()
            val trialLength = if (trialLengthInt != null && trialLengthInt > 0) trialLengthInt else null

            val oneTimeShipping =
                subscriptionInformation[SubscriptionDetailsKeys.SUBSCRIPTION_ONE_TIME_SHIPPING] == "yes"

            SubscriptionDetails(
                price = price,
                period = period,
                periodInterval = periodInterval,
                length = length,
                signUpFee = signUpFee,
                trialPeriod = trialPeriod,
                trialLength = trialLength,
                oneTimeShipping = oneTimeShipping
            )
        } else null
    }
}

object MetadataKeys {
    const val ID = "id"
    const val KEY = "key"
    const val VALUE = "value"
}

object SubscriptionDetailsKeys {
    const val SUBSCRIPTION_PRICE = "_subscription_price"
    const val SUBSCRIPTION_PERIOD = "_subscription_period"
    const val SUBSCRIPTION_PERIOD_INTERVAL = "_subscription_period_interval"
    const val SUBSCRIPTION_LENGTH = "_subscription_length"
    const val SUBSCRIPTION_SIGN_UP_FEE = "_subscription_sign_up_fee"
    const val SUBSCRIPTION_TRIAL_PERIOD = "_subscription_trial_period"
    const val SUBSCRIPTION_TRIAL_LENGTH = "_subscription_trial_length"
    const val SUBSCRIPTION_ONE_TIME_SHIPPING = "_subscription_one_time_shipping"
    val Keys = listOf(
        SUBSCRIPTION_PRICE,
        SUBSCRIPTION_TRIAL_LENGTH,
        SUBSCRIPTION_SIGN_UP_FEE,
        SUBSCRIPTION_PERIOD,
        SUBSCRIPTION_PERIOD_INTERVAL,
        SUBSCRIPTION_LENGTH,
        SUBSCRIPTION_TRIAL_PERIOD,
        SUBSCRIPTION_ONE_TIME_SHIPPING
    )
}
