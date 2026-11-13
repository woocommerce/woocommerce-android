package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class CustomerWithAnalyticsTest {
    @Test
    fun `given shipping address with phone, when getShippingAddress, then phone is the last line`() {
        // GIVEN
        val shippingAddress = Address.EMPTY.copy(firstName = "Sam", address1 = "99 Shipping Ave", phone = "2125550199")
        val customer = customerWithShippingAddress(shippingAddress)

        // WHEN
        val result = customer.getShippingAddress(formatPhone = { "formatted $it" })

        // THEN
        assertThat(result).isEqualTo("${shippingAddress.getFullAddress()}\nformatted 2125550199")
    }

    @Test
    fun `given shipping address without phone, when getShippingAddress, then only address is shown`() {
        // GIVEN
        val shippingAddress = Address.EMPTY.copy(firstName = "Sam", address1 = "99 Shipping Ave", phone = " ")
        val customer = customerWithShippingAddress(shippingAddress)

        // WHEN
        val result = customer.getShippingAddress(formatPhone = { "formatted $it" })

        // THEN
        assertThat(result).isEqualTo(shippingAddress.getFullAddress())
    }

    private fun customerWithShippingAddress(shippingAddress: Address) = CustomerWithAnalytics(
        remoteCustomerId = 1L,
        analyticsCustomerId = null,
        firstName = "Sam",
        lastName = "",
        username = "sam",
        email = "sam@example.com",
        phone = "2125550100",
        lastActive = null,
        ordersCount = null,
        totalSpend = null,
        averageOrderValue = null,
        registeredDate = "2024-01-01",
        billingAddress = Address.EMPTY.copy(phone = "2125550100"),
        shippingAddress = shippingAddress
    )
}
