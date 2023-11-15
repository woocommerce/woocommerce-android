package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCMetaData
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
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

// Currently, only subscription details metadata is editable from the app. The rest is read only and thus
// not included in the persisted metadata.
fun SubscriptionDetails.toMetadataJson(): String {
    val jsonArray = JsonArray()
    val subscriptionValues = mapOf(
        SubscriptionMetadataKeys.SUBSCRIPTION_PRICE to price,
        SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD to period.value,
        SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD_INTERVAL to periodInterval,
        SubscriptionMetadataKeys.SUBSCRIPTION_LENGTH to length,
        SubscriptionMetadataKeys.SUBSCRIPTION_SIGN_UP_FEE to signUpFee,
        SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_PERIOD to trialPeriod?.value,
        SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_LENGTH to trialLength,
        SubscriptionMetadataKeys.SUBSCRIPTION_ONE_TIME_SHIPPING to oneTimeShipping
    )
    subscriptionValues.forEach { (key, value) ->
        jsonArray.add(
            JsonObject().also { json ->
                json.addProperty(WCMetaData.KEY, key)
                json.addProperty(WCMetaData.VALUE, value.toString())
            }
        )
    }
    return jsonArray.toString()
}
