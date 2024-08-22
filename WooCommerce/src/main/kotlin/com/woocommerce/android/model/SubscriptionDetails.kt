package com.woocommerce.android.model

import android.os.Parcelable
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.WCProductModel.SubscriptionMetadataKeys
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import java.math.BigDecimal

@Parcelize
data class SubscriptionDetails(
    val price: BigDecimal?,
    val period: SubscriptionPeriod,
    val periodInterval: Int,
    val length: Int?,
    val signUpFee: BigDecimal?,
    val trialPeriod: SubscriptionPeriod?,
    val trialLength: Int?,
    val oneTimeShipping: Boolean,
    val paymentsSyncDate: SubscriptionPaymentSyncDate?
) : Parcelable {
    /**
     * Returns true when free trial is disabled and renewal payments are not synced
     */
    val supportsOneTimeShipping: Boolean
        get() = (trialLength == null || trialLength == 0) &&
            (paymentsSyncDate == null || paymentsSyncDate == SubscriptionPaymentSyncDate.None)
}

fun SubscriptionDetails.toMetadataJson(): JsonArray {
    val subscriptionValues = mapOf(
        SubscriptionMetadataKeys.SUBSCRIPTION_PRICE to price?.toString().orEmpty(),
        SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD to period.value,
        SubscriptionMetadataKeys.SUBSCRIPTION_PERIOD_INTERVAL to periodInterval,
        SubscriptionMetadataKeys.SUBSCRIPTION_LENGTH to (length ?: 0),
        SubscriptionMetadataKeys.SUBSCRIPTION_SIGN_UP_FEE to signUpFee?.toString().orEmpty(),
        SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_PERIOD to (trialPeriod ?: SubscriptionPeriod.Day).value,
        SubscriptionMetadataKeys.SUBSCRIPTION_TRIAL_LENGTH to (trialLength ?: 0),
        SubscriptionMetadataKeys.SUBSCRIPTION_ONE_TIME_SHIPPING to if (oneTimeShipping) "yes" else "no",
    )
    val jsonArray = JsonArray()
    subscriptionValues.forEach { (key, value) ->
        jsonArray.add(
            JsonObject().also { json ->
                json.addProperty(WCMetaData.KEY, key)
                json.addProperty(WCMetaData.VALUE, value.toString())
            }
        )
    }
    return jsonArray
}
