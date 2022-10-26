package com.woocommerce.android.iap.pub

import com.android.billingclient.api.ProductDetails
import org.mockito.kotlin.mock

fun buildProductDetails(currency: String): ProductDetails {
    val phaseList = mock<ProductDetails.PricingPhase> {
        on { priceCurrencyCode }.thenReturn(currency)
    }

    val pricingPhases = mock<ProductDetails.PricingPhases> {
        on { this.pricingPhaseList }.thenReturn(listOf(phaseList))
    }

    val subscriptionOfferDetails = mock<ProductDetails.SubscriptionOfferDetails> {
        on { this.pricingPhases }.thenReturn(pricingPhases)
    }

    return mock {
        on { this.subscriptionOfferDetails }.thenReturn(listOf(subscriptionOfferDetails))
    }
}
