package org.wordpress.android.fluxc.model.shippinglabels

import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress

sealed class WCAddressVerificationResult {
    data class Valid(
        val suggestedAddress: ShippingLabelAddress,
        val isTrivialNormalization: Boolean
    ) : WCAddressVerificationResult()
    data class InvalidAddress(val message: String) : WCAddressVerificationResult()
    data class InvalidRequest(val message: String) : WCAddressVerificationResult()
}
