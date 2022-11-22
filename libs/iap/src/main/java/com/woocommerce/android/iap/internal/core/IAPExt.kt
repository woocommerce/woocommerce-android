package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClient.BillingResponseCode
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.woocommerce.android.iap.internal.model.IAPProduct
import com.woocommerce.android.iap.internal.model.IAPPurchase

internal val ProductDetails.priceOfTheFirstPurchasedOfferInMicros
    get() = subscriptionOfferDetails?.get(0)!!.pricingPhases.pricingPhaseList[0]!!.priceAmountMicros

internal val ProductDetails.currencyOfTheFirstPurchasedOffer
    get() = subscriptionOfferDetails?.get(0)!!.pricingPhases.pricingPhaseList[0]!!.priceCurrencyCode

internal val ProductDetails.firstOfferToken
    get() = subscriptionOfferDetails!!.first().offerToken

internal val BillingResult.isSuccess
    get() = this.responseCode == BillingResponseCode.OK

internal fun List<IAPPurchase>?.isProductPurchased(iapProduct: IAPProduct) =
    findPurchaseWithProduct(iapProduct)?.state == IAPPurchase.State.PURCHASED

internal fun List<IAPPurchase>?.isProductAcknowledged(iapProduct: IAPProduct) =
    findPurchaseWithProduct(iapProduct)?.isAcknowledged == true

internal fun List<IAPPurchase>?.findPurchaseWithProduct(iapProduct: IAPProduct) =
    this?.find { it.products.find { iapProduct.productId == it.id } != null }
