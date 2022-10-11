package com.woocommerce.android.iap.pub.model

sealed class IAPBillingErrorType(val debugMessage: String) {
    class UserCancelled(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ItemAlreadyOwned(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class DeveloperError(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ServiceDisconnected(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class BillingUnavailable(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ItemUnavailable(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class FeatureNotSupported(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ServiceTimeout(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ItemNotOwned(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class ServiceUnavailable(debugMessage: String) : IAPBillingErrorType(debugMessage)
    class Unknown(debugMessage: String) : IAPBillingErrorType(debugMessage)
}
