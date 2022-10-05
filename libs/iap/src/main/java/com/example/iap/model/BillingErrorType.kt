package com.example.iap.model

sealed class BillingErrorType(val debugMessage: String) {
    class UserCancelled(debugMessage: String) : BillingErrorType(debugMessage)
    class ItemAlreadyOwned(debugMessage: String) : BillingErrorType(debugMessage)
    class DeveloperError(debugMessage: String) : BillingErrorType(debugMessage)
    class ServiceDisconnected(debugMessage: String) : BillingErrorType(debugMessage)
    class BillingUnavailable(debugMessage: String) : BillingErrorType(debugMessage)
    class ItemUnavailable(debugMessage: String) : BillingErrorType(debugMessage)
    class FeatureNotSupported(debugMessage: String) : BillingErrorType(debugMessage)
    class ServiceTimeout(debugMessage: String) : BillingErrorType(debugMessage)
    class ItemNotOwned(debugMessage: String) : BillingErrorType(debugMessage)
    class ServiceUnavailable(debugMessage: String) : BillingErrorType(debugMessage)
    class Unknown(debugMessage: String) : BillingErrorType(debugMessage)
}
