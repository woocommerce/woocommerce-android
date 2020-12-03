package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.Error
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.Invalid
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.NotRecognized
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult.Valid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

class ShippingLabelAddressValidator @Inject constructor(
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite
) {
    suspend fun validateAddress(address: Address, type: AddressType): ValidationResult {
        val result = withContext(Dispatchers.IO) {
            shippingLabelStore.verifyAddress(selectedSite.get(), address.toShippingLabelAddress(), type.toDataType())
        }

        return if (result.isError) {
            // TODO: Add analytics
            Error(result.error.type)
        } else {
            when (result.model) {
                null -> NotRecognized
                is InvalidRequest, is InvalidAddress -> NotRecognized
                is WCAddressVerificationResult.Valid -> {
                    val suggestion = (result.model as WCAddressVerificationResult.Valid).suggestedAddress.toAppModel()
                    if (suggestion != address) {
                        Invalid(suggestion)
                    } else {
                        Valid
                    }
                }
            }
        }
    }

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val suggested: Address) : ValidationResult()
        object NotRecognized : ValidationResult()
        data class Error(val type: WooErrorType) : ValidationResult()
    }

    enum class AddressType {
        ORIGIN,
        DESTINATION;

        fun toDataType(): Type {
            return when (this) {
                ORIGIN -> Type.ORIGIN
                DESTINATION -> Type.DESTINATION
            }
        }
    }

    private fun Address.toShippingLabelAddress(): ShippingLabelAddress {
        return ShippingLabelAddress(
            company = company,
            name = "$firstName $lastName",
            phone = phone,
            country = country,
            state = state,
            address = address1,
            address2 = address2,
            city = city,
            postcode = postcode
        )
    }
}
