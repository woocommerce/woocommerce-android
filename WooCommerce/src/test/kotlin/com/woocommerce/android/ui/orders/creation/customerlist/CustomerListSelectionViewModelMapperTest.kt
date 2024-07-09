package com.woocommerce.android.ui.orders.creation.customerlist

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListViewState.CustomerList.Item.Customer
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.model.order.OrderAddress

class CustomerListSelectionViewModelMapperTest {
    private val textHandler: CustomerListDisplayTextHandler = mock()
    private val mapper = CustomerListViewModelMapper(textHandler)

    @Test
    fun `when mapFromWCCustomer, then return view model customer model`() {
        // GIVEN
        val wcCustomerModel: WCCustomerModel = mock {
            on { remoteCustomerId }.thenReturn(1)
            on { firstName }.thenReturn("firstName")
            on { lastName }.thenReturn("lastName")
            on { username }.thenReturn("username")
            on { email }.thenReturn("email")
        }

        val highlightedName = Customer.Text.Highlighted(
            "firstName lastName",
            0,
            17
        )
        whenever(
            textHandler.invoke(
                CustomerListDisplayTextHandler.CustomerParam.Name("firstName lastName"),
                "",
                CustomerListDisplayTextHandler.SearchType.ALL,
            )
        ).thenReturn(highlightedName)

        val highlightedEmail = Customer.Text.Highlighted(
            "email",
            0,
            17
        )
        whenever(
            textHandler.invoke(
                CustomerListDisplayTextHandler.CustomerParam.Email("email"),
                "",
                CustomerListDisplayTextHandler.SearchType.ALL,
            )
        ).thenReturn(highlightedEmail)

        val highlightedUsername = Customer.Text.Highlighted(
            "username",
            0,
            17
        )
        whenever(
            textHandler.invoke(
                CustomerListDisplayTextHandler.CustomerParam.Username("username"),
                "",
                CustomerListDisplayTextHandler.SearchType.ALL,
            )
        ).thenReturn(highlightedUsername)

        // WHEN
        val result = mapper.mapFromWCCustomerToItem(
            wcCustomerModel,
            "",
            CustomerListDisplayTextHandler.SearchType.ALL
        )

        // THEN
        assertThat(result.remoteId).isEqualTo(1)
        assertThat(result.name).isEqualTo(highlightedName)
        assertThat(result.email).isEqualTo(highlightedEmail)
        assertThat(result.username).isEqualTo(highlightedUsername)
    }

    @Test
    fun `given billing address, when mapFromOrderAddressToAddress, then return address model`() {
        // GIVEN
        val address = OrderAddress.Billing(
            company = "company",
            lastName = "lastName",
            firstName = "firstName",
            address1 = "address1",
            address2 = "address2",
            email = "email",
            postcode = "postcode",
            phone = "phone",
            country = "country",
            state = "state",
            city = "city"
        )
        val country: Location = mock()
        val state: Location = mock()

        // WHEN
        val result = mapper.mapFromOrderAddressToAddress(address, country, state)

        // THEN
        assertThat(result.company).isEqualTo("company")
        assertThat(result.lastName).isEqualTo("lastName")
        assertThat(result.firstName).isEqualTo("firstName")
        assertThat(result.address1).isEqualTo("address1")
        assertThat(result.address2).isEqualTo("address2")
        assertThat(result.email).isEqualTo("email")
        assertThat(result.postcode).isEqualTo("postcode")
        assertThat(result.phone).isEqualTo("phone")
        assertThat(result.country).isEqualTo(country)
        assertThat(result.state).isEqualTo(AmbiguousLocation.Defined(state))
        assertThat(result.city).isEqualTo("city")
    }

    @Test
    fun `given shipping address, when mapFromOrderAddressToAddress, then return address model`() {
        // GIVEN
        val address = OrderAddress.Shipping(
            company = "company",
            lastName = "lastName",
            firstName = "firstName",
            address1 = "address1",
            address2 = "address2",
            postcode = "postcode",
            phone = "phone",
            country = "country",
            state = "state",
            city = "city"
        )
        val country: Location = mock()
        val state: Location = mock()

        // WHEN
        val result = mapper.mapFromOrderAddressToAddress(address, country, state)

        // THEN
        assertThat(result.company).isEqualTo("company")
        assertThat(result.lastName).isEqualTo("lastName")
        assertThat(result.firstName).isEqualTo("firstName")
        assertThat(result.address1).isEqualTo("address1")
        assertThat(result.address2).isEqualTo("address2")
        assertThat(result.email).isEqualTo("")
        assertThat(result.postcode).isEqualTo("postcode")
        assertThat(result.phone).isEqualTo("phone")
        assertThat(result.country).isEqualTo(country)
        assertThat(result.state).isEqualTo(AmbiguousLocation.Defined(state))
        assertThat(result.city).isEqualTo("city")
    }

    @Test
    fun `when mapFromCustomerModelToShippingAddress, then shipping address returned`() {
        // GIVEN
        val customerModel: WCCustomerModel = mock {
            on { shippingCompany }.thenReturn("company")
            on { shippingAddress1 }.thenReturn("address1")
            on { shippingAddress2 }.thenReturn("address2")
            on { shippingCity }.thenReturn("city")
            on { shippingState }.thenReturn("state")
            on { shippingCountry }.thenReturn("country")
            on { shippingPostcode }.thenReturn("postcode")
            on { shippingFirstName }.thenReturn("firstName")
            on { shippingLastName }.thenReturn("lastName")
        }

        // WHEN
        val result = mapper.mapFromCustomerModelToShippingAddress(customerModel)

        // THEN
        assertThat(result.company).isEqualTo("company")
        assertThat(result.lastName).isEqualTo("lastName")
        assertThat(result.firstName).isEqualTo("firstName")
        assertThat(result.address1).isEqualTo("address1")
        assertThat(result.address2).isEqualTo("address2")
        assertThat(result.postcode).isEqualTo("postcode")
        assertThat(result.phone).isEqualTo("")
        assertThat(result.country).isEqualTo("country")
        assertThat(result.state).isEqualTo("state")
    }

    @Test
    fun `when mapFromCustomerModelToBillingAddress, then billing address returned`() {
        // GIVEN
        val customerModel: WCCustomerModel = mock {
            on { billingCompany }.thenReturn("company")
            on { billingAddress1 }.thenReturn("address1")
            on { billingAddress2 }.thenReturn("address2")
            on { billingCity }.thenReturn("city")
            on { billingState }.thenReturn("state")
            on { billingCountry }.thenReturn("country")
            on { billingPostcode }.thenReturn("postcode")
            on { billingFirstName }.thenReturn("firstName")
            on { billingLastName }.thenReturn("lastName")
            on { billingEmail }.thenReturn("email")
            on { billingPhone }.thenReturn("phone")
        }

        // WHEN
        val result = mapper.mapFromCustomerModelToBillingAddress(customerModel)

        // THEN
        assertThat(result.company).isEqualTo("company")
        assertThat(result.lastName).isEqualTo("lastName")
        assertThat(result.firstName).isEqualTo("firstName")
        assertThat(result.address1).isEqualTo("address1")
        assertThat(result.address2).isEqualTo("address2")
        assertThat(result.postcode).isEqualTo("postcode")
        assertThat(result.phone).isEqualTo("phone")
        assertThat(result.country).isEqualTo("country")
        assertThat(result.state).isEqualTo("state")
        assertThat(result.email).isEqualTo("email")
    }
}
