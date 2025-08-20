package com.woocommerce.android.ui.orders.wooshippinglabels.address

import com.woocommerce.android.extensions.isNotNullOrEmpty
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.models.AddressNormalizationModel

data class EditAddressViewState(
    val isCompanyExpanded: Boolean,
    val editableAddress: EditableAddress,
    val loading: LoadingState,
    val error: EditAddressError?,
    val shouldUseStatesInput: Boolean,
    val addressStatus: AddressStatus,
    val addressValidationState: AddressValidationState
)

sealed class LoadingState {
    data object Hidden : LoadingState()
    data class DisplayLoading(val title: String, val message: String) : LoadingState()
}

data class EditAddressError(val message: String, val isIndefinite: Boolean = true, val onRetry: () -> Unit)

sealed class AddressStatus {
    data object Verified : AddressStatus()
    data object Unverified : AddressStatus()
    data object MissingAddress : AddressStatus()
    data object MissingInfo : AddressStatus()
    data object SaveChanges : AddressStatus()
    data class VerifyFailed(val exception: NormalizeAddressException? = null) : AddressStatus()
}

sealed class AddressValidationState {
    data object NotStarted : AddressValidationState()
    data object VerifyingAddress : AddressValidationState()
    data class VerificationFailed(
        val editableAddress: EditableAddress,
        val exception: NormalizeAddressException? = null,
    ) : AddressValidationState()

    data class AddressSelection(
        val addressNormalization: AddressNormalizationModel,
        val selectedAddress: Address
    ) : AddressValidationState()

    data object UpdatingAddress : AddressValidationState()
    data class AddressUpdateFailed(
        val editableAddress: EditableAddress
    ) : AddressValidationState()

    data class NormalizedAddressUpdateFailed(
        val selection: AddressSelection,
    ) : AddressValidationState()
}

data class EditableAddress(
    val name: InputValue = InputValue.EMPTY,
    val company: InputValue = InputValue.EMPTY,
    val country: Location = Location.EMPTY,
    val address: InputValue = InputValue.EMPTY,
    val city: InputValue = InputValue.EMPTY,
    val state: Location = Location.EMPTY,
    val postalCode: InputValue = InputValue.EMPTY,
    val email: InputValue = InputValue.EMPTY,
    val phone: InputValue = InputValue.EMPTY
) {
    val hasIncorrectOrMissingData: Boolean
        get() = address.error.isNotNullOrEmpty() ||
            city.error.isNotNullOrEmpty() ||
            postalCode.error.isNotNullOrEmpty() ||
            email.error.isNotNullOrEmpty() ||
            phone.error.isNotNullOrEmpty() ||
            name.error.isNotNullOrEmpty() ||
            company.error.isNotNullOrEmpty()
}

fun EditableAddress.toAddress() = Address(
    firstName = name.value,
    lastName = "",
    company = company.value,
    address1 = address.value,
    address2 = "",
    city = city.value,
    state = AmbiguousLocation.Defined(state),
    postcode = postalCode.value,
    country = country,
    email = email.value,
    phone = phone.value
)

data class InputValue(val value: String, val error: String? = null, val isRequired: Boolean = false) {
    companion object {
        val EMPTY = InputValue("")
    }
}

sealed class LocationState {
    data object Loading : LocationState()
    data object DisplayLoading : LocationState()
    data object Error : LocationState()
    data class Loaded(val locations: List<Location>) : LocationState()
}
