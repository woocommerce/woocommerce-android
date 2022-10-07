package com.woocommerce.android.iap.internal.core

import com.android.billingclient.api.BillingClient.BillingResponseCode.BILLING_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.DEVELOPER_ERROR
import com.android.billingclient.api.BillingClient.BillingResponseCode.FEATURE_NOT_SUPPORTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_NOT_OWNED
import com.android.billingclient.api.BillingClient.BillingResponseCode.ITEM_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.OK
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_DISCONNECTED
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_TIMEOUT
import com.android.billingclient.api.BillingClient.BillingResponseCode.SERVICE_UNAVAILABLE
import com.android.billingclient.api.BillingClient.BillingResponseCode.USER_CANCELED
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.ProductDetailsResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.Purchase.PurchaseState
import com.woocommerce.android.iap.internal.model.IAPProductDetailsResponse
import com.woocommerce.android.iap.public.model.BillingErrorType
import com.woocommerce.android.iap.public.model.IAPPurchase
import com.woocommerce.android.iap.public.model.IAPPurchase.State
import com.woocommerce.android.iap.public.model.IAPPurchase.State.PENDING
import com.woocommerce.android.iap.public.model.IAPPurchase.State.PURCHASED

internal class IAPOutMapper {
    fun mapProductDetailsResultToIAPProductDetailsResponse(productDetailsResult: ProductDetailsResult) =
        when (productDetailsResult.billingResult.responseCode) {
            OK -> IAPProductDetailsResponse.Success(productDetailsResult.productDetailsList.orEmpty())
            else -> IAPProductDetailsResponse.Error(
                mapBillingResultErrorToBillingResultType(
                    productDetailsResult.billingResult
                )
            )
        }

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
            price = productDetails.getPriceOfTheFirstPurchasedOfferInMicros,
            currency = productDetails.getCurrencyOfTheFirstPurchasedOffer
        )

    fun mapBillingResultErrorToBillingResultType(billingResult: BillingResult) =
        when (billingResult.responseCode) {
            USER_CANCELED -> BillingErrorType.UserCancelled(billingResult.debugMessage)
            ITEM_ALREADY_OWNED -> BillingErrorType.ItemAlreadyOwned(billingResult.debugMessage)
            DEVELOPER_ERROR -> BillingErrorType.DeveloperError(billingResult.debugMessage)
            SERVICE_DISCONNECTED -> BillingErrorType.ServiceDisconnected(billingResult.debugMessage)
            BILLING_UNAVAILABLE -> BillingErrorType.BillingUnavailable(billingResult.debugMessage)
            ITEM_UNAVAILABLE -> BillingErrorType.ItemUnavailable(billingResult.debugMessage)
            FEATURE_NOT_SUPPORTED -> BillingErrorType.FeatureNotSupported(billingResult.debugMessage)
            SERVICE_TIMEOUT -> BillingErrorType.ServiceTimeout(billingResult.debugMessage)
            SERVICE_UNAVAILABLE -> BillingErrorType.ServiceUnavailable(billingResult.debugMessage)
            ITEM_NOT_OWNED -> BillingErrorType.ItemNotOwned(billingResult.debugMessage)
            else -> {
                BillingErrorType.Unknown(billingResult.debugMessage)
            }
        }

    private fun mapPurchaseStateToIAPPurchaseState(@PurchaseState purchaseState: Int) =
        when (purchaseState) {
            PurchaseState.PURCHASED -> PURCHASED
            PurchaseState.PENDING -> PENDING
            else -> State.UNSPECIFIED_STATE
        }
}
