package com.woocommerce.android.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCProductVariationModel.SubscriptionMetadataKeys
import java.math.BigDecimal

object SubscriptionDetailsMapper {
    private val gson by lazy { Gson() }
    fun toAppModel(metadata: String): SubscriptionDetails? {
        val jsonArray = gson.fromJson(metadata, JsonArray::class.java)

        val subscriptionInformation = jsonArray
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject -> jsonObject[WCMetaData.KEY].asString in SubscriptionMetadataKeys.ALL_KEYS }
            .associate { jsonObject ->
                jsonObject[WCMetaData.KEY].asString to jsonObject[WCMetaData.VALUE].asString
            }

        return if (subscriptionInformation.isNotEmpty()) {
            val price = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PRICE]?.toBigDecimalOrNull()
                ?: BigDecimal.ZERO

            val periodString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD] ?: ""
            val period = SubscriptionPeriod.fromValue(periodString)

            val periodIntervalString =
                subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD_INTERVAL] ?: ""
            val periodInterval = periodIntervalString.toIntOrNull() ?: 0

            val lengthString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_LENGTH] ?: ""
            val lengthInt = lengthString.toIntOrNull()
            val length = if (lengthInt != null && lengthInt > 0) lengthInt else null

            val signUpFee =
                subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_SIGN_UP_FEE]?.toBigDecimalOrNull()

            val trialPeriodString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_PERIOD]
            val trialPeriod = trialPeriodString?.let { SubscriptionPeriod.fromValue(trialPeriodString) }

            val trialLengthString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_LENGTH] ?: ""
            val trialLengthInt = trialLengthString.toIntOrNull()
            val trialLength = if (trialLengthInt != null && trialLengthInt > 0) trialLengthInt else null

            val oneTimeShipping =
                subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_ONE_TIME_SHIPPING] == "yes"

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
