package com.woocommerce.android.ui.orders.shippinglabels.creation

import com.woocommerce.android.model.*
import com.woocommerce.android.model.ShippingLabelPackage.Item
import java.math.BigDecimal
import java.util.Date

object CreateShippingLabelTestUtils {
    val testCountry = Location(
        code = "US",
        name = "USA",
    )

    val testState = AmbiguousLocation.Defined(
        Location(
            parentCode = "US",
            code = "CA",
            name = "California",
        )
    )
    fun generateAddress(): Address {
        return Address(
            company = "KFC",
            firstName = "Harland",
            lastName = "Sanders",
            phone = "12345678",
            country = testCountry,
            state = testState,
            address1 = "123 Main St.",
            address2 = "",
            city = "Lexington",
            postcode = "11222",
            email = "boss@kfc.com"
        )
    }

    fun generatePackage(id: String = "id", provider: String = "provider"): ShippingPackage {
        return ShippingPackage(
            id, "title1", false, provider, PackageDimensions(1.0f, 1.0f, 1.0f), 1f
        )
    }

    fun generateIndividualPackage(): ShippingPackage {
        return ShippingPackage(
            id = ShippingPackage.INDIVIDUAL_PACKAGE,
            title = ShippingPackage.INDIVIDUAL_PACKAGE,
            category = ShippingPackage.INDIVIDUAL_PACKAGE,
            isLetter = false,
            dimensions = PackageDimensions(1.0f, 1.0f, 1.0f),
            boxWeight = 0f
        )
    }

    fun generateShippingLabelPackage(
        position: Int = 1,
        weight: Float = 10f,
        selectedPackage: ShippingPackage? = null,
        items: List<Item>? = null
    ): ShippingLabelPackage {
        return ShippingLabelPackage(
            position,
            selectedPackage ?: generatePackage(),
            weight,
            items ?: listOf(Item(0L, "product", "", 2, 10f, BigDecimal.valueOf(10L)))
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
            formattedPrice = "10 $",
            discount = BigDecimal.ZERO,
            formattedFee = "10 $",
            option = ShippingRate.Option.DEFAULT
        )
    }

    fun generateCustomsPackage(packageId: String = "package1"): CustomsPackage {
        return CustomsPackage(
            id = packageId,
            labelPackage = generateShippingLabelPackage(),
            returnToSender = true,
            contentsType = ContentsType.Merchandise,
            contentsDescription = null,
            restrictionType = RestrictionType.None,
            itn = "",
            lines = listOf(
                CustomsLine(
                    productId = 1L,
                    itemDescription = "product",
                    hsTariffNumber = "",
                    quantity = 1,
                    value = BigDecimal.valueOf(10),
                    weight = 1f,
                    originCountry = Location("US", "United States (US)")
                )
            )
        )
    }
}
