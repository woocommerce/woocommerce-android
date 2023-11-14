package com.woocommerce.android.ui.orders.shippinglabels.creation

import android.os.Parcelable
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.ShippingLabelAddressMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.DESTINATION
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType.ORIGIN
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidAddress
import org.wordpress.android.fluxc.model.shippinglabels.WCAddressVerificationResult.InvalidRequest
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel.ShippingLabelAddress.Type
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.WCShippingLabelStore
import javax.inject.Inject

class ShippingLabelAddressValidator @Inject constructor(
    private val shippingLabelStore: WCShippingLabelStore,
    private val selectedSite: SelectedSite,
    private val shippingLabelAddressMapper: ShippingLabelAddressMapper,
) {
    suspend fun validateAddress(
        address: Address,
        type: AddressType,
        isCustomsFormRequired: Boolean
    ): ValidationResult {
        return when {
            isNameMissing(address) -> ValidationResult.NameMissing
            !address.phone.isValidPhoneNumber(type, isCustomsFormRequired) -> ValidationResult.PhoneInvalid
            else -> verifyAddress(address, type)
        }
    }

    private suspend fun verifyAddress(address: Address, type: AddressType): ValidationResult {
        val result = withContext(Dispatchers.IO) {
            shippingLabelStore.verifyAddress(
                selectedSite.get(),
                address.toShippingLabelModel(),
                type.toDataType()
            )
        }

        if (result.isError) {
            AnalyticsTracker.track(
                AnalyticsEvent.SHIPPING_LABEL_ADDRESS_VALIDATION_FAILED,
                mapOf("error" to result.error.type.name)
            )

            return ValidationResult.Error(result.error.type)
        }
        return when (val validationResult = result.model) {
            null -> {
                AnalyticsTracker.track(
                    AnalyticsEvent.SHIPPING_LABEL_ADDRESS_VALIDATION_FAILED,
                    mapOf("error" to "response_model_null")
                )

                ValidationResult.Error(GENERIC_ERROR)
            }

            is InvalidRequest -> {
                AnalyticsTracker.track(
                    AnalyticsEvent.SHIPPING_LABEL_ADDRESS_VALIDATION_FAILED,
                    mapOf("error" to "address_not_found")
                )

                ValidationResult.NotFound(validationResult.message)
            }

            is InvalidAddress -> {
                AnalyticsTracker.track(
                    AnalyticsEvent.SHIPPING_LABEL_ADDRESS_VALIDATION_FAILED,
                    mapOf("error" to "invalid_address")
                )

                ValidationResult.Invalid(validationResult.message)
            }

            is WCAddressVerificationResult.Valid -> {
                AnalyticsTracker.track(AnalyticsEvent.SHIPPING_LABEL_ADDRESS_VALIDATION_SUCCEEDED)
                val suggestion =
                    validationResult.suggestedAddress.let { shippingLabelAddressMapper.toAppModel(it) }
                if (suggestion.toString() != address.toString()) {
                    ValidationResult.SuggestedChanges(suggestion, validationResult.isTrivialNormalization)
                } else {
                    ValidationResult.Valid
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
        object PhoneInvalid : ValidationResult()

        @Parcelize
        data class SuggestedChanges(val suggested: Address, val isTrivial: Boolean) : ValidationResult()

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
}

/**
 * Checks whether the phone number is valid or not, depending on the [addressType] and [isCustomsFormRequired].
 * The logic of the check is:
 * - [ORIGIN]:
 *    - If a customs form is required, then checks whether the phone number contains 10 digits exactly after deleting
 *      an optional 1 as the area code.
 *      As EasyPost is permissive for the presence of other characters, we delete all other characters before checking,
 *      and that's similar to what the web client does.
 *      Source: https://github.com/Automattic/woocommerce-services/issues/1351
 *    - If no customs form is required, then checks whether the phone number is not blank.
 *      Check ticket for discussion on why https://github.com/woocommerce/woocommerce-android/issues/8578
 *
 * - [DESTINATION]:
 *   - If a customs form is required, then checks whether the phone has any digits.
 *   - If no customs form is required, then the phone number not required, so no validation is needed.
 */
@Suppress("MagicNumber")
fun String.isValidPhoneNumber(addressType: AddressType, isCustomsFormRequired: Boolean): Boolean {
    return when (addressType) {
        ORIGIN -> {
            if (isCustomsFormRequired) {
                replace(Regex("^1|[^\\d]"), "").length == 10
            } else {
                isNotBlank()
            }
        }

        DESTINATION -> {
            if (isCustomsFormRequired) {
                contains(Regex("\\d"))
            } else {
                true
            }
        }
    }
}
