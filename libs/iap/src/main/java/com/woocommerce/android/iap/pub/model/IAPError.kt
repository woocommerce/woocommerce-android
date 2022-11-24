package com.woocommerce.android.iap.pub.model

sealed class IAPError {
    sealed class Billing(val debugMessage: String) : IAPError() {
        class UserCancelled(debugMessage: String) : Billing(debugMessage)
        class ItemAlreadyOwned(debugMessage: String) : Billing(debugMessage)
        class DeveloperError(debugMessage: String) : Billing(debugMessage)
        class ServiceDisconnected(debugMessage: String) : Billing(debugMessage)
        class BillingUnavailable(debugMessage: String) : Billing(debugMessage)
        class ItemUnavailable(debugMessage: String) : Billing(debugMessage)
        class FeatureNotSupported(debugMessage: String) : Billing(debugMessage)
        class ServiceTimeout(debugMessage: String) : Billing(debugMessage)
        class ItemNotOwned(debugMessage: String) : Billing(debugMessage)
        class ServiceUnavailable(debugMessage: String) : Billing(debugMessage)
        class Unknown(debugMessage: String) : Billing(debugMessage)
    }

    sealed class RemoteCommunication : IAPError() {
        object Network : RemoteCommunication()
        data class Server(val reason: String) : RemoteCommunication()
    }
}
