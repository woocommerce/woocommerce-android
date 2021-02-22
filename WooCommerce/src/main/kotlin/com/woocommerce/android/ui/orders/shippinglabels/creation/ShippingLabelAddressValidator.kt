package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.tools.SelectedSite
import kotlinx.android.parcel.Parcelize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

class ShippingLabelAddressValidator @Inject constructor(
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite
) {
    suspend fun validateAddress(address: Address, type: AddressType): ValidationResult {
        if (isNameMissing(address)) {
            return ValidationResult.NameMissing
        } else {
            val result = withContext(Dispatchers.IO) {
                shippingLabelStore.verifyAddress(
                    selectedSite.get(),
                    address.toShippingLabelAddress(),
                    type.toDataType()
                )
            }

            return if (result.isError) {
                // TODO: Add analytics
                ValidationResult.Error(result.error.type)
            } else when (result.model) {
                null -> ValidationResult.Error(GENERIC_ERROR)
                is InvalidRequest -> ValidationResult.NotFound((result.model as InvalidRequest).message)
                is InvalidAddress -> ValidationResult.Invalid((result.model as InvalidAddress).message)
                is WCAddressVerificationResult.Valid -> {
                    val suggestion = (result.model as WCAddressVerificationResult.Valid).suggestedAddress.toAppModel()
                    if (suggestion.toString() != address.toString()) {
                        ValidationResult.SuggestedChanges(suggestion)
                    } else {
                        ValidationResult.Valid
                    }
                }
            }
        }
    }

    private fun isNameMissing(address: Address): Boolean {
        return (address.firstName + address.lastName).isBlank() && address.company.isBlank()
    }

    sealed class ValidationResult : Parcelable {
        @Parcelize
        object Valid : ValidationResult()

        @Parcelize
        object NameMissing : ValidationResult()

        @Parcelize
        data class SuggestedChanges(val suggested: Address) : ValidationResult()

        @Parcelize
        data class Invalid(val message: String) : ValidationResult()

        @Parcelize
        data class NotFound(val message: String) : ValidationResult()

        @Parcelize
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
