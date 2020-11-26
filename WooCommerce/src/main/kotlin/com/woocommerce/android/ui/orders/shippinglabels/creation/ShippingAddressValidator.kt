package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingAddressValidator.ValidationResult.Valid
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

class ShippingAddressValidator @Inject constructor(
    private val shippingLabelStore: WCShippingLabelStore
) {
    fun validateAddress(address: Address): ValidationResult {
        return Valid
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        object Invalid : ValidationResult()
        object NotRecognized : ValidationResult()
    }
}