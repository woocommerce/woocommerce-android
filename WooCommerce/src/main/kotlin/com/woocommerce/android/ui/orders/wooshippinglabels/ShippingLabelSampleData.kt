package com.woocommerce.android.ui.orders.wooshippinglabels

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.wooshippinglabels.models.DestinationShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.OriginShippingAddress
import com.woocommerce.android.ui.orders.wooshippinglabels.models.PaymentMethodModel

object ShippingLabelSampleData {
    fun getShipFrom() = OriginShippingAddress(
        firstName = "first name",
        lastName = "last name",
        company = "Company",
        phone = "",
        address1 = "A huge address that should be truncated",
        address2 = "",
        city = "City",
        postcode = "",
        email = "email",
        country = "USA",
        state = "California",
        id = "id_1",
        isDefault = true,
        isVerified = true
    )

    fun getShipTo() = DestinationShippingAddress(
        address = Address(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "",
            address1 = "Another Address",
            address2 = "",
            city = "City",
            postcode = "",
            email = "email",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA")),
        ),
        isVerified = true
    )

    fun getShippingLines(number: Int = 3) = List(number) { i ->
        ShippingLineSummaryUI(
            title = "Shipping $i",
            amount = "$12.99"
        )
    }

    fun getPaymentsSection() = PaymentsSectionUI(
        selectedPaymentMethod = PaymentMethodModel(
            paymentMethodId = 1,
            name = "Visa",
            cardType = "VISA",
            cardDigits = "1234",
            expiry = "12/25"
        )
    )
}
