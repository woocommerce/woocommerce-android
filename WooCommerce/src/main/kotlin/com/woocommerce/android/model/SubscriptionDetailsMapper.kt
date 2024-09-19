package com.woocommerce.android.model

import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaData

object SubscriptionDetailsMapper {
    private val gson by lazy { Gson() }
    fun toAppModel(metadata: String): SubscriptionDetails? {
        val jsonArray = gson.fromJson(metadata, JsonArray::class.java) ?: return null

        val subscriptionInformation = jsonArray
            .mapNotNull { it as? JsonObject }
            .filter { jsonObject -> jsonObject[WCMetaData.KEY].asString in SubscriptionMetadataKeys.ALL_KEYS }
            .associate { jsonObject ->
                jsonObject[WCMetaData.KEY].asString to jsonObject[WCMetaData.VALUE]
            }

        return if (subscriptionInformation.isNotEmpty()) {
            val price = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PRICE]?.asString
                ?.toBigDecimalOrNull()

            val periodString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD]?.asString ?: ""
            val period = SubscriptionPeriod.fromValue(periodString)

            val periodIntervalString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD_INTERVAL]
                ?.asString ?: ""
            val periodInterval = periodIntervalString.toIntOrNull() ?: 0

            val lengthInt = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_LENGTH]?.asString
                ?.toIntOrNull()
            val length = if (lengthInt != null && lengthInt > 0) lengthInt else null

            val signUpFee = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_SIGN_UP_FEE]?.asString
                ?.toBigDecimalOrNull()

            val trialPeriodString = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_PERIOD]
                ?.asString
            val trialPeriod = trialPeriodString?.let { SubscriptionPeriod.fromValue(trialPeriodString) }

            val trialLengthInt = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_LENGTH]
                ?.asString?.toIntOrNull()
            val trialLength = if (trialLengthInt != null && trialLengthInt > 0) trialLengthInt else null

            val oneTimeShipping = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_ONE_TIME_SHIPPING]
                ?.asString == "yes"

            val paymentsSyncDate = subscriptionInformation[SubscriptionMetadataKeys.SUBSCRIPTION_PAYMENT_SYNC_DATE]
                ?.extractPaymentsSyncDate()

            SubscriptionDetails(
                price = price,
                period = period,
                periodInterval = periodInterval,
                length = length,
                signUpFee = signUpFee,
                trialPeriod = trialPeriod,
                trialLength = trialLength,
                oneTimeShipping = oneTimeShipping,
                paymentsSyncDate = paymentsSyncDate
            )
        } else {
            null
        }
    }

    private fun JsonElement.extractPaymentsSyncDate(): SubscriptionPaymentSyncDate? {
        return when {
            isJsonObject -> asJsonObject.let {
                val day = it["day"].asInt
                val month = it["month"].asInt
                if (day == 0) {
                    SubscriptionPaymentSyncDate.None
                } else {
                    SubscriptionPaymentSyncDate.MonthDay(day, month)
                }
            }

            else -> asString?.toIntOrNull()?.let { day ->
                if (day == 0) {
                    SubscriptionPaymentSyncDate.None
                } else {
                    SubscriptionPaymentSyncDate.Day(day)
                }
            }
        }
    }
}
