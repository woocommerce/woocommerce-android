package com.woocommerce.android.ui.orders.creation.views

import android.view.View
import androidx.core.view.isVisible
import com.woocommerce.android.databinding.LayoutAddressFormBinding
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.details.editing.address.AddressViewModel

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

val LayoutAddressFormBinding?.textFieldsState
    get() = Address(
        company = this?.company?.text.orEmpty(),
        firstName = this?.firstName?.text.orEmpty(),
        lastName = this?.lastName?.text.orEmpty(),
        phone = this?.phone?.text.orEmpty(),
        country = Location.EMPTY,
        state = AmbiguousLocation.EMPTY,
        address1 = this?.address1?.text.orEmpty(),
        address2 = this?.address2?.text.orEmpty(),
        city = this?.city?.text.orEmpty(),
        postcode = this?.postcode?.text.orEmpty(),
        email = this?.email?.text.orEmpty()
    )

fun LayoutAddressFormBinding?.inflateTextFields(address: Address) {
    this?.city?.text = address.city
    this?.company?.text = address.company
    this?.firstName?.text = address.firstName
    this?.lastName?.text = address.lastName
    this?.phone?.text = address.phone
    this?.address1?.text = address.address1
    this?.address2?.text = address.address2
    this?.postcode?.text = address.postcode
    this?.email?.text = address.email
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
            this?.stateEditText?.text = stateLocation.value
        }
    }
}

fun LayoutAddressFormBinding?.update(state: AddressViewModel.AddressSelectionState) {
    this?.inflateTextFields(state.address)
    this?.inflateLocationFields(countryLocation = state.address.country, stateLocation = state.address.state)
    this?.updateLocationStateViews(state.stateSpinnerStatus)
}
