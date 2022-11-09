package com.woocommerce.android.iap.pub

import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
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
    title: String = "Title",
    description: String = "Description",
): ProductDetails {
    val phaseList = mock<ProductDetails.PricingPhase> {
        on { priceCurrencyCode }.thenReturn(currency)
        on { priceAmountMicros }.thenReturn(price)
        on { billingCycleCount }.thenReturn(1)
        on { recurrenceMode }.thenReturn(1)
        on { billingPeriod }.thenReturn("1")
        on { formattedPrice }.thenReturn("1")
    }

    val pricingPhases = mock<ProductDetails.PricingPhases> {
        on { this.pricingPhaseList }.thenReturn(listOf(phaseList))
    }

    val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails> {
        on { this.pricingPhases }.thenReturn(pricingPhases)
        on { this.offerToken }.thenReturn("offerToken")
        on { this.offerTags }.thenReturn(listOf("tag"))
    }

    return mock {
        on { this.subscriptionOfferDetails }.thenReturn(listOf(subscriptionOfferDetails))
        on { this.productId }.thenReturn(productId)
        on { this.name }.thenReturn(name)
        on { this.title }.thenReturn(title)
        on { this.description }.thenReturn(description)
        on { this.productType }.thenReturn(ProductType.SUBS)
    }
}

fun buildPurchase(
    products: List<String>,
    @PurchaseState purchaseState: Int,
    isAcknowledged: Boolean = true,
    purchaseToken: String = ""
): Purchase {
    return mock {
        on { this.purchaseState }.thenReturn(purchaseState)
        on { this.orderId }.thenReturn("orderId")
        on { this.products }.thenReturn(products)
        on { this.isAcknowledged }.thenReturn(isAcknowledged)
        on { this.isAutoRenewing }.thenReturn(false)
        on { this.developerPayload }.thenReturn("developerPayload")
        on { this.purchaseToken }.thenReturn(purchaseToken)
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
