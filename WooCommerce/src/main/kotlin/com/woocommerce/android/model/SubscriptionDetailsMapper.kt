package com.woocommerce.android.model

import com.google.gson.Gson
import com.google.gson.JsonParser
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.get

object SubscriptionDetailsMapper {
    private val gson by lazy { Gson() }
    fun toAppModel(metadata: List<WCMetaData>): SubscriptionDetails? {
        if (metadata.none { it.key in SubscriptionMetadataKeys.ALL_KEYS }) {
            return null
        }

        val price = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_PRICE]?.valueAsString?.toBigDecimalOrNull()

        val periodString = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD]?.valueAsString ?: ""
        val period = SubscriptionPeriod.fromValue(periodString)

        val periodIntervalString = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD_INTERVAL]
            ?.valueAsString ?: ""
        val periodInterval = periodIntervalString.toIntOrNull() ?: 0

        val lengthInt = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_LENGTH]?.valueAsString
            ?.toIntOrNull()
        val length = if (lengthInt != null && lengthInt > 0) lengthInt else null

        val signUpFee = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_SIGN_UP_FEE]?.valueAsString
            ?.toBigDecimalOrNull()

        val trialPeriodString = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_PERIOD]
            ?.valueAsString
        val trialPeriod = trialPeriodString?.let { SubscriptionPeriod.fromValue(trialPeriodString) }

        val trialLengthInt = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_LENGTH]
            ?.valueAsString?.toIntOrNull()
        val trialLength = if (trialLengthInt != null && trialLengthInt > 0) trialLengthInt else null

        val oneTimeShipping = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_ONE_TIME_SHIPPING]
            ?.valueAsString == "yes"

        val paymentsSyncDate = metadata[SubscriptionMetadataKeys.SUBSCRIPTION_PAYMENT_SYNC_DATE]
            ?.extractPaymentsSyncDate()

        return SubscriptionDetails(
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
    }

    fun toAppModel(metadata: String): SubscriptionDetails? {
        val metadataList = gson.fromJson(metadata, Array<WCMetaData>::class.java)?.toList() ?: return null

        return toAppModel(metadataList)
    }

    private fun WCMetaData.extractPaymentsSyncDate(): SubscriptionPaymentSyncDate? {
        return when(isJson) {
            true -> value.stringValue.let {
                val jsonObject = JsonParser.parseString(it).asJsonObject
                val day = jsonObject["day"].asInt
                val month = jsonObject["month"].asInt
                if (day == 0) {
                    SubscriptionPaymentSyncDate.None
                } else {
                    SubscriptionPaymentSyncDate.MonthDay(month = month, day = day)
                }
            }

            false -> valueAsString.toIntOrNull()?.let { day ->
                if (day == 0) {
                    SubscriptionPaymentSyncDate.None
                } else {
                    SubscriptionPaymentSyncDate.Day(day)
                }
            }
        }
    }
}
