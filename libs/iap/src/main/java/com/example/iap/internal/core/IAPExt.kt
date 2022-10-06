package com.example.iap.internal.core

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.PurchasesResult

// TODO support for multiple plans?
val ProductDetails.getPriceOfTheFirstPurchasedOfferInMicros
    get() = subscriptionOfferDetails?.get(0)!!.pricingPhases.pricingPhaseList[0]!!.priceAmountMicros

val ProductDetails.getCurrencyOfTheFirstPurchasedOffer
    get() = subscriptionOfferDetails?.get(0)!!.pricingPhases.pricingPhaseList[0]!!.priceCurrencyCode

val PurchasesResult.isSuccess
    get() = this.billingResult.responseCode == BillingResponseCode.OK
