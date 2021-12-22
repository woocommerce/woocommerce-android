package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomsPackage
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderId
import com.woocommerce.android.model.ShippingLabel
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingRate
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.AddressType
import com.woocommerce.android.ui.orders.shippinglabels.creation.ShippingLabelAddressValidator.ValidationResult
import com.woocommerce.android.viewmodel.MultiLiveEvent
import org.wordpress.android.fluxc.model.order.OrderIdentifier

sealed class CreateShippingLabelEvent : MultiLiveEvent.Event() {
    data class ShowAddressEditor(
        val address: Address,
        val type: AddressType,
        val validationResult: ValidationResult?,
        val requiresPhoneNumber: Boolean
    ) : CreateShippingLabelEvent()

    data class ShowSuggestedAddress(
        val originalAddress: Address,
        val suggestedAddress: Address,
        val type: AddressType
    ) : CreateShippingLabelEvent()

    data class UseSelectedAddress(val address: Address) : CreateShippingLabelEvent()

    data class EditSelectedAddress(val address: Address) : CreateShippingLabelEvent()

    data class ShowCountrySelector(
        val locations: List<Location>,
        val currentCountryCode: String?
    ) : CreateShippingLabelEvent()

    data class ShowStateSelector(
        val locations: List<Location>,
        val currentStateCode: String?
    ) : CreateShippingLabelEvent()

    data class OpenMapWithAddress(
        val address: Address
    ) : CreateShippingLabelEvent()

    data class DialPhoneNumber(
        val phoneNumber: String
    ) : CreateShippingLabelEvent()

    data class ShowPackageDetails(
        val orderIdentifier: OrderIdentifier,
        val shippingLabelPackages: List<ShippingLabelPackage>
    ) : CreateShippingLabelEvent()

    data class ShowCustomsForm(
        val originCountryCode: String,
        val destinationCountryCode: String,
        val shippingPackages: List<ShippingLabelPackage>,
        val customsPackages: List<CustomsPackage>
    ) : CreateShippingLabelEvent()

    data class ShowShippingRates(
        val order: Order,
        val originAddress: Address,
        val destinationAddress: Address,
        val shippingLabelPackages: List<ShippingLabelPackage>,
        val customsPackages: List<CustomsPackage>?,
        val selectedRates: List<ShippingRate>
    ) : CreateShippingLabelEvent()

    object ShowPaymentDetails : CreateShippingLabelEvent()

    data class ShowPrintShippingLabels(val orderId: OrderId, val labels: List<ShippingLabel>) :
        CreateShippingLabelEvent()

    object ShowWooDiscountBottomSheet : CreateShippingLabelEvent()
}
