package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR
import com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.woocommerce.android.iap.internal.model.IAPPurchase
import com.woocommerce.android.iap.pub.model.IAPError

internal class IAPOutMapper {
    fun mapPurchaseWithProductDetailsToIAPPurchase(
        purchase: Purchase,
        productDetails: List<ProductDetails>
    ) = IAPPurchase(
        orderId = purchase.orderId,
        products = productDetails.map { mapProductDetailsToIAPProduct(it) },
        isAcknowledged = purchase.isAcknowledged,
        isAutoRenewing = purchase.isAutoRenewing,
        state = mapPurchaseStateToIAPPurchaseState(purchase.purchaseState),
        developerPayload = purchase.developerPayload,
        purchaseToken = purchase.purchaseToken,
        signature = purchase.signature
    )

    private fun mapProductDetailsToIAPProduct(productDetails: ProductDetails) =
        IAPPurchase.Product(
            id = productDetails.productId,
            name = productDetails.name,
            price = productDetails.priceOfTheFirstPurchasedOfferInMicros,
            currency = productDetails.currencyOfTheFirstPurchasedOffer
        )

    fun mapBillingResultErrorToBillingResultType(billingResult: BillingResult) =
        when (billingResult.responseCode) {
            USER_CANCELED -> IAPError.Billing.UserCancelled(billingResult.debugMessage)
            ITEM_ALREADY_OWNED -> IAPError.Billing.ItemAlreadyOwned(billingResult.debugMessage)
            DEVELOPER_ERROR -> IAPError.Billing.DeveloperError(billingResult.debugMessage)
            SERVICE_DISCONNECTED -> IAPError.Billing.ServiceDisconnected(billingResult.debugMessage)
            BILLING_UNAVAILABLE -> IAPError.Billing.BillingUnavailable(billingResult.debugMessage)
            ITEM_UNAVAILABLE -> IAPError.Billing.ItemUnavailable(billingResult.debugMessage)
            FEATURE_NOT_SUPPORTED -> IAPError.Billing.FeatureNotSupported(billingResult.debugMessage)
            SERVICE_TIMEOUT -> IAPError.Billing.ServiceTimeout(billingResult.debugMessage)
            SERVICE_UNAVAILABLE -> IAPError.Billing.ServiceUnavailable(billingResult.debugMessage)
            ITEM_NOT_OWNED -> IAPError.Billing.ItemNotOwned(billingResult.debugMessage)
            else -> {
                IAPError.Billing.Unknown(billingResult.debugMessage)
            }
        }

    private fun mapPurchaseStateToIAPPurchaseState(@PurchaseState purchaseState: Int) =
        when (purchaseState) {
            PurchaseState.PURCHASED -> IAPPurchase.State.PURCHASED
            PurchaseState.PENDING -> IAPPurchase.State.PENDING
            else -> IAPPurchase.State.UNSPECIFIED_STATE
        }
}
