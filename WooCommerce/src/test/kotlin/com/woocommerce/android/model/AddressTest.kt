package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class AddressTest {
    @Test
    fun `when name is blank, then nullify the name of data model`() {
        val address = Address(
            firstName = "",
            lastName = " ",
            company = "Company",
            phone = "",
            address1 = "Address 1",
            address2 = "",
            city = "City",
            postcode = "",
            email = "email",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
        )

        val dataModel = address.toShippingLabelModel()

        assertThat(dataModel.name).isNull()
    }

    @Test
    fun `given addresses differing only by email, when compared ignoring email, then they are the same`() {
        val billing = Address(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "123456789",
            address1 = "Address 1",
            address2 = "",
            city = "City",
            postcode = "90210",
            email = "customer@example.com",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
        )
        val shipping = billing.copy(email = "")

        assertThat(shipping.isSameIgnoringEmail(billing)).isTrue
    }

    @Test
    fun `given addresses with a different field, when compared ignoring email, then they are not the same`() {
        val billing = Address(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "123456789",
            address1 = "Address 1",
            address2 = "",
            city = "City",
            postcode = "90210",
            email = "customer@example.com",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
        )
        val shipping = billing.copy(email = "", city = "Other city")

        assertThat(shipping.isSameIgnoringEmail(billing)).isFalse
    }

    @Test
    fun `when first and last name are not blank, then concatenate them for the data model`() {
        val address = Address(
            firstName = "first name",
            lastName = "last name",
            company = "Company",
            phone = "",
            address1 = "Address 1",
            address2 = "",
            city = "City",
            postcode = "",
            email = "email",
            country = Location("US", "USA"),
            state = AmbiguousLocation.Defined(Location("CA", "California", "USA"))
        )

        val dataModel = address.toShippingLabelModel()

        assertThat(dataModel.name).isEqualTo("${address.firstName} ${address.lastName}")
    }
}
