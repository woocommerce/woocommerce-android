package com.woocommerce.android.ui.orders.creation.views

import android.view.View
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel

fun LayoutAddressFormBinding.bindEditFields(
    addressType: AddressViewModel.AddressType,
    onFieldEdited: (AddressViewModel.AddressType, AddressViewModel.Field, String) -> Unit
) {
    mapOf(
        firstName to AddressViewModel.Field.FirstName,
        lastName to AddressViewModel.Field.LastName,
        company to AddressViewModel.Field.Company,
        address1 to AddressViewModel.Field.Address1,
        address2 to AddressViewModel.Field.Address2,
        phone to AddressViewModel.Field.Phone,
        city to AddressViewModel.Field.City,
        postcode to AddressViewModel.Field.Zip,
        stateEditText to AddressViewModel.Field.State,
        email to AddressViewModel.Field.Email
    ).onEach { (editText, field) ->
        editText.setOnTextChangedListener { onFieldEdited(addressType, field, it?.toString().orEmpty()) }
    }
}

fun LayoutAddressFormBinding?.updateLocationStateViews(hasStatesAvailable: AddressViewModel.StateSpinnerStatus) {
    when (hasStatesAvailable) {
        AddressViewModel.StateSpinnerStatus.HAVING_LOCATIONS -> {
            this?.stateSpinner?.isVisible = true
            this?.stateEditText?.isVisible = false
            this?.stateSpinner?.isEnabled = true
        }
        AddressViewModel.StateSpinnerStatus.RAW_VALUE -> {
            this?.stateSpinner?.isVisible = false
            this?.stateEditText?.isVisible = true
        }
        AddressViewModel.StateSpinnerStatus.DISABLED -> {
            this?.stateSpinner?.isVisible = true
            this?.stateEditText?.isVisible = false
            this?.stateSpinner?.isEnabled = false
        }
    }
}

fun LayoutAddressFormBinding?.inflateTextFields(address: Address) {
    this?.city?.setTextIfDifferent(address.city)
    this?.company?.setTextIfDifferent(address.company)
    this?.firstName?.setTextIfDifferent(address.firstName)
    this?.lastName?.setTextIfDifferent(address.lastName)
    this?.phone?.setTextIfDifferent(address.phone)
    this?.address1?.setTextIfDifferent(address.address1)
    this?.address2?.setTextIfDifferent(address.address2)
    this?.postcode?.setTextIfDifferent(address.postcode)
    this?.email?.setTextIfDifferent(address.email)
}

fun LayoutAddressFormBinding?.inflateLocationFields(countryLocation: Location, stateLocation: AmbiguousLocation) {
    this?.countrySpinner?.setText(countryLocation.name)
    when (stateLocation) {
        is AmbiguousLocation.Defined -> {
            this?.stateSpinner?.visibility = View.VISIBLE
            this?.stateEditText?.visibility = View.GONE
            this?.stateSpinner?.setText(stateLocation.value.name)
        }
        is AmbiguousLocation.Raw -> {
            this?.stateSpinner?.visibility = View.GONE
            this?.stateEditText?.visibility = View.VISIBLE
            this?.stateEditText?.setTextIfDifferent(stateLocation.value)
        }
    }
}

fun LayoutAddressFormBinding?.update(state: AddressViewModel.AddressSelectionState) {
    this?.inflateTextFields(state.address)
    this?.inflateLocationFields(countryLocation = state.address.country, stateLocation = state.address.state)
    this?.updateLocationStateViews(state.stateSpinnerStatus)
}
