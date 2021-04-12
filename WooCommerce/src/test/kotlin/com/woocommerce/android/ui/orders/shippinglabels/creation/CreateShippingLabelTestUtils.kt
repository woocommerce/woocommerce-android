package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.PackageDimensions
import com.woocommerce.android.model.PaymentMethod
import com.woocommerce.android.model.ShippingLabelPackage
import com.woocommerce.android.model.ShippingLabelPackage.Item
import com.woocommerce.android.model.ShippingPackage
import com.woocommerce.android.model.ShippingRate
import java.math.BigDecimal
import java.util.Date

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

    fun generatePackage(id: String = "id", provider: String = "provider"): ShippingPackage {
        return ShippingPackage(
            id, "title1", false, provider, PackageDimensions(1.0f, 1.0f, 1.0f)
        )
    }

    fun generateShippingLabelPackage(
        id: String = "package1",
        weight: Float = 10f,
        selectedPackage: ShippingPackage? = null
    ): ShippingLabelPackage {
        return ShippingLabelPackage(
            id,
            selectedPackage ?: generatePackage(),
            weight,
            listOf(Item(0L, "product", "", "10 kg"))
        )
    }

    fun generatePaymentMethod(id: Int = 1, cardType: String = "visa"): PaymentMethod {
        return PaymentMethod(id, "Jhon Doe", cardType, "1234", Date(2030, 11, 31))
    }

    fun generateRate(packageId: String = "package1"): ShippingRate {
        return ShippingRate(
            packageId = packageId,
            shipmentId = "shipmentId",
            rateId = "rateId",
            serviceId = "serviceId",
            carrierId = "carrier",
            serviceName = "service",
            deliveryDays = 10,
            price = BigDecimal.TEN,
            discount = BigDecimal.ZERO,
            formattedFee = "10 $",
            option = ShippingRate.Option.DEFAULT
        )
    }
}
