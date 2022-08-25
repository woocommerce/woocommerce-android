package com.woocommerce.android.ui.orders.details

import android.util.Log
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import javax.inject.Inject

class AddressValidator @Inject constructor(
    private val coroutineDispatchers: CoroutineDispatchers,
    private val shippingLabelAddressValidator: ShippingLabelAddressValidator,
    private val orderDetailRepository: OrderDetailRepository
) {
    suspend fun validate(address: Address) {
        val hasValidFields = fieldValidation(address)
        if (hasValidFields.not()) {
            Log.d("Address", "missing required fields")
        }
        withContext(coroutineDispatchers.io) {
            val plugin = orderDetailRepository.getWooServicesPluginInfo()
            if (plugin.isInstalled && plugin.isActive) {
                val validationResult = shippingLabelAddressValidator.validateAddress(
                    address = address,
                    type = ShippingLabelAddressValidator.AddressType.DESTINATION,
                    requiresPhoneNumber = true
                )
                when (validationResult) {
                    ShippingLabelAddressValidator.ValidationResult.Valid -> {
                        Log.d("Address", "valid address")
                    }
                    is ShippingLabelAddressValidator.ValidationResult.SuggestedChanges -> {
                        Log.d("Address", "address with suggestions")
                    }
                    else -> {
                        Log.d("Address", "invalid address")
                    }
                }
            } else {
                WooResult(hasValidFields)
            }
        }
    }

    private fun fieldValidation(address: Address): Boolean {
        return with(address) {
            when {
                // The name or the company name should not be empty
                firstName.isEmpty() && lastName.isEmpty() || company.isEmpty() -> false
                address1.isEmpty() -> false
                city.isEmpty() -> false
                postcode.isEmpty() -> false
                state.isNotEmpty().not() -> false
                country == Location.EMPTY -> false
                // The phone should be a number
                phone.contains(Regex("\\d")).not() -> false
                else -> true
            }
        }
    }
}
