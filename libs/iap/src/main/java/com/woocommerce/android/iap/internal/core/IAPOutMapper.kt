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
import com.woocommerce.android.iap.public.model.IAPBillingErrorType
import com.woocommerce.android.iap.public.model.IAPProductInfo
import com.woocommerce.android.iap.public.model.IAPPurchase
import com.woocommerce.android.iap.public.model.IAPPurchase.State
import com.woocommerce.android.iap.public.model.IAPPurchase.State.PENDING
import com.woocommerce.android.iap.public.model.IAPPurchase.State.PURCHASED

private const val MILLION = 1_000_000

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

    fun mapProductDetailsToIAPProductInfo(productDetails: ProductDetails) =
        IAPProductInfo(
            localizedTitle = productDetails.title,
            localizedDescription = productDetails.description,
            // TODO return currenct and amount separatly so it can be handled in the UI properly?
            displayPrice = "${productDetails.priceOfTheFirstPurchasedOfferInMicros / MILLION}" +
                productDetails.currencyOfTheFirstPurchasedOffer
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
            USER_CANCELED -> IAPBillingErrorType.UserCancelled(billingResult.debugMessage)
            ITEM_ALREADY_OWNED -> IAPBillingErrorType.ItemAlreadyOwned(billingResult.debugMessage)
            DEVELOPER_ERROR -> IAPBillingErrorType.DeveloperError(billingResult.debugMessage)
            SERVICE_DISCONNECTED -> IAPBillingErrorType.ServiceDisconnected(billingResult.debugMessage)
            BILLING_UNAVAILABLE -> IAPBillingErrorType.BillingUnavailable(billingResult.debugMessage)
            ITEM_UNAVAILABLE -> IAPBillingErrorType.ItemUnavailable(billingResult.debugMessage)
            FEATURE_NOT_SUPPORTED -> IAPBillingErrorType.FeatureNotSupported(billingResult.debugMessage)
            SERVICE_TIMEOUT -> IAPBillingErrorType.ServiceTimeout(billingResult.debugMessage)
            SERVICE_UNAVAILABLE -> IAPBillingErrorType.ServiceUnavailable(billingResult.debugMessage)
            ITEM_NOT_OWNED -> IAPBillingErrorType.ItemNotOwned(billingResult.debugMessage)
            else -> {
                IAPBillingErrorType.Unknown(billingResult.debugMessage)
            }
        }

    private fun mapPurchaseStateToIAPPurchaseState(@PurchaseState purchaseState: Int) =
        when (purchaseState) {
            PurchaseState.PURCHASED -> PURCHASED
            PurchaseState.PENDING -> PENDING
            else -> State.UNSPECIFIED_STATE
        }
}
