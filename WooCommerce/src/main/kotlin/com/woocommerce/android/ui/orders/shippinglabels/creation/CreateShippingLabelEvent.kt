package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.wordpress.android.fluxc.model.data.WCLocationModel

sealed class CreateShippingLabelEvent : MultiLiveEvent.Event() {
    data class ShowAddressEditor(
        val address: Address,
        val type: AddressType,
        val validationResult: ValidationResult?
    ) : CreateShippingLabelEvent()

    data class ShowSuggestedAddress(
        val originalAddress: Address,
        val suggestedAddress: Address
    ) : CreateShippingLabelEvent()

    object CancelAddressEditing : CreateShippingLabelEvent()

    data class ShowCountrySelector(
        val locations: List<WCLocationModel>,
        val currentCountry: String?
    ) : CreateShippingLabelEvent()
}
