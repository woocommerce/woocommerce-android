package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import org.mockito.kotlin.mock

fun buildProductDetails(
    productId: String,
    name: String,
    price: Long,
    currency: String,
): ProductDetails {
    val phaseList = mock<ProductDetails.PricingPhase> {
        on { priceCurrencyCode }.thenReturn(currency)
        on { priceAmountMicros }.thenReturn(price)
    }

    val pricingPhases = mock<ProductDetails.PricingPhases> {
        on { this.pricingPhaseList }.thenReturn(listOf(phaseList))
    }

    val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails> {
        on { this.pricingPhases }.thenReturn(pricingPhases)
    }

    return mock {
        on { this.subscriptionOfferDetails }.thenReturn(listOf(subscriptionOfferDetails))
        on { this.productId }.thenReturn(productId)
        on { this.name }.thenReturn(name)
    }
}

fun buildPurchase(
    products: List<String>,
    @PurchaseState purchaseState: Int
): Purchase {
    return mock {
        on { this.purchaseState }.thenReturn(purchaseState)
        on { this.orderId }.thenReturn("orderId")
        on { this.products }.thenReturn(products)
        on { this.isAcknowledged }.thenReturn(false)
        on { this.isAutoRenewing }.thenReturn(false)
        on { this.developerPayload }.thenReturn("developerPayload")
        on { this.purchaseToken }.thenReturn("purchaseToken")
        on { this.signature }.thenReturn("signature")
    }
}

fun buildBillingResult(
    @BillingClient.BillingResponseCode responseCode: Int,
    debugMessage: String = ""
) = BillingResult
    .newBuilder()
    .setResponseCode(responseCode)
    .setDebugMessage(debugMessage)
    .build()
