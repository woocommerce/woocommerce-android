package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address

object CreateShippingLabelTestUtils {
    fun generateAddress(): Address {
        return Address(
            company = "KFC",
            firstName = "Harland",
            lastName = "Sanders",
            phone = "12345678",
            country = "US",
            state = "KY",
            address1 = "123 Main St.",
            address2 = "",
            city = "Lexington",
            postcode = "11222",
            email = "boss@kfc.com"
        )
    }
}
