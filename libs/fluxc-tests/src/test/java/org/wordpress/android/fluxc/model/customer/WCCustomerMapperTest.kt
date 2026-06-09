package org.wordpress.android.fluxc.model.customer

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerFromAnalyticsDTO
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WCCustomerMapperTest {
    val mapper = WCCustomerMapper()

    @Test
    @Suppress("LongMethod")
    fun `mapper maps to correct customer model`() {
        // given
        val remoteId = 13L
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val billing = CustomerDTO.Billing(
            address1 = "billingAddress1",
            address2 = "billingAddress2",
            city = "billingCity",
            company = "billingCompany",
            country = "billingCountry",
            email = "billingEmail",
            firstName = "billingFirstName",
            lastName = "billingLastName",
            phone = "billingPhone",
            postcode = "billingPostcode",
            state = "billingState"
        )
        val shipping = CustomerDTO.Shipping(
            address1 = "shippingAddress1",
            address2 = "shippingAddress2",
            city = "shippingCity",
            company = "shippingCompany",
            country = "shippingCountry",
            firstName = "shippingFirstName",
            lastName = "shippingLastName",
            postcode = "shippingPostcode",
            state = "shippingState"
        )
        val response = CustomerDTO(
            avatarUrl = "avatarUrl",
            billing = billing,
            dateCreated = "dateCreated",
            dateCreatedGmt = "dateCreatedGmt",
            dateModified = "dateModified",
            dateModifiedGmt = "dateModifiedGmt",
            email = "email",
            firstName = "firstName",
            id = remoteId,
            isPayingCustomer = true,
            lastName = "lastName",
            role = "role",
            shipping = shipping,
            username = "username"
        )

        // when
        val result = mapper.mapToModel(site, response)

        // then
        with(result) {
            assertEquals("avatarUrl", avatarUrl)
            assertEquals("dateCreated", dateCreated)
            assertEquals("dateCreatedGmt", dateCreatedGmt)
            assertEquals("dateModified", dateModified)
            assertEquals("dateModifiedGmt", dateModifiedGmt)
            assertEquals("email", email)
            assertEquals("firstName", firstName)
            assertEquals(remoteCustomerId.value, remoteId)
            assertTrue(isPayingCustomer)
            assertEquals("lastName", lastName)
            assertEquals("role", role)
            assertEquals("username", username)

            assertEquals("shippingAddress1", shippingAddress1)
            assertEquals("shippingAddress2", shippingAddress2)
            assertEquals("shippingCity", shippingCity)
            assertEquals("shippingCompany", shippingCompany)
            assertEquals("shippingCountry", shippingCountry)
            assertEquals("shippingFirstName", shippingFirstName)
            assertEquals("shippingLastName", shippingLastName)
            assertEquals("shippingPostcode", shippingPostcode)
            assertEquals("shippingState", shippingState)

            assertEquals("billingAddress1", billingAddress1)
            assertEquals("billingAddress2", billingAddress2)
            assertEquals("billingCity", billingCity)
            assertEquals("billingCompany", billingCompany)
            assertEquals("billingCountry", billingCountry)
            assertEquals("billingFirstName", billingFirstName)
            assertEquals("billingLastName", billingLastName)
            assertEquals("billingPostcode", billingPostcode)
            assertEquals("billingState", billingState)
            assertEquals("billingEmail", billingEmail)
            assertEquals("billingPhone", billingPhone)
        }
    }

    @Test
    @Suppress("LongMethod")
    fun `mapper maps to correct customer dto`() {
        // given
        val model = WCCustomerModel(
            avatarUrl = "avatarUrl",
            dateCreated = "dateCreated",
            dateCreatedGmt = "dateCreatedGmt",
            dateModified = "dateModified",
            dateModifiedGmt = "dateModifiedGmt",
            email = "email",
            firstName = "firstName",
            isPayingCustomer = true,
            lastName = "lastName",
            role = "role",
            username = "username",

            billingAddress1 = "billingAddress1",
            billingAddress2 = "billingAddress2",
            billingCity = "billingCity",
            billingCompany = "billingCompany",
            billingCountry = "billingCountry",
            billingEmail = "billingEmail",
            billingFirstName = "billingFirstName",
            billingLastName = "billingLastName",
            billingPhone = "billingPhone",
            billingPostcode = "billingPostcode",
            billingState = "billingState",

            shippingAddress1 = "shippingAddress1",
            shippingAddress2 = "shippingAddress2",
            shippingCity = "shippingCity",
            shippingCompany = "shippingCompany",
            shippingCountry = "shippingCountry",
            shippingFirstName = "shippingFirstName",
            shippingLastName = "shippingLastName",
            shippingPostcode = "shippingPostcode",
            shippingState = "shippingState",
        )

        // when
        val result = mapper.mapToDTO(model)

        // then
        with(result) {
            assertNull(avatarUrl)
            assertNull(dateCreated)
            assertNull(dateCreatedGmt)
            assertNull(dateModified)
            assertNull(dateModifiedGmt)
            assertEquals("email", email)
            assertEquals("firstName", firstName)
            assertFalse(isPayingCustomer)
            assertEquals("lastName", lastName)
            assertNull(role)
            assertEquals("username", username)

            assertEquals("shippingAddress1", shipping?.address1)
            assertEquals("shippingAddress2", shipping?.address2)
            assertEquals("shippingCity", shipping?.city)
            assertEquals("shippingCompany", shipping?.company)
            assertEquals("shippingCountry", shipping?.country)
            assertEquals("shippingFirstName", shipping?.firstName)
            assertEquals("shippingLastName", shipping?.lastName)
            assertEquals("shippingPostcode", shipping?.postcode)
            assertEquals("shippingState", shipping?.state)

            assertEquals("billingAddress1", billing?.address1)
            assertEquals("billingAddress2", billing?.address2)
            assertEquals("billingCity", billing?.city)
            assertEquals("billingCompany", billing?.company)
            assertEquals("billingCountry", billing?.country)
            assertEquals("billingFirstName", billing?.firstName)
            assertEquals("billingLastName", billing?.lastName)
            assertEquals("billingPostcode", billing?.postcode)
            assertEquals("billingState", billing?.state)
            assertEquals("billingEmail", billing?.email)
            assertEquals("billingPhone", billing?.phone)
        }
    }

    @Test
    fun `given total spend more than 0, when mapToModel, then maps to correct model with  paying true`() {
        // GIVEN
        val remoteId = 13L
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val response = CustomerFromAnalyticsDTO(
            avgOrderValue = 1.0,
            city = "city",
            country = "country",
            dateLastActive = "dateLastActive",
            dateLastActiveGmt = "dateLastActiveGmt",
            dateLastOrder = "dateLastOrder",
            dateRegistered = "dateRegistered",
            dateRegisteredGmt = "dateRegisteredGmt",
            email = "email",
            id = remoteId,
            name = "firstname lastname",
            ordersCount = 1,
            postcode = "postcode",
            state = "state",
            totalSpend = 1.0,
            userId = 1L
        )

        // WHEN
        val result = mapper.mapToModel(site, response)

        // THEN
        assertThat(result.firstName).isEqualTo("firstname")
        assertThat(result.lastName).isEqualTo("lastname")
        assertThat(result.email).isEqualTo("email")
        assertThat(result.isPayingCustomer).isEqualTo(true)
        assertThat(result.remoteCustomerId.value).isEqualTo(1L)
        assertThat(result.dateCreated).isEqualTo("dateRegistered")
        assertThat(result.dateModified).isEqualTo("dateLastActive")

        assertThat(result.billingCountry).isEqualTo("country")
        assertThat(result.shippingCountry).isEqualTo("country")

        assertThat(result.billingCity).isEqualTo("city")
        assertThat(result.shippingCity).isEqualTo("city")

        assertThat(result.shippingFirstName).isEqualTo("firstname")
        assertThat(result.shippingLastName).isEqualTo("lastname")
        assertThat(result.billingFirstName).isEqualTo("firstname")
        assertThat(result.billingLastName).isEqualTo("lastname")

        assertThat(result.billingPostcode).isEqualTo("postcode")
        assertThat(result.shippingPostcode).isEqualTo("postcode")

        assertThat(result.billingState).isEqualTo("state")
        assertThat(result.shippingState).isEqualTo("state")
    }

    @Test
    fun `given total spend 0, when mapToModel, then maps to correct model with paying false`() {
        // GIVEN
        val remoteId = 13L
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val response = CustomerFromAnalyticsDTO(
            avgOrderValue = 1.0,
            city = "city",
            country = "country",
            dateLastActive = "dateLastActive",
            dateLastActiveGmt = "dateLastActiveGmt",
            dateLastOrder = "dateLastOrder",
            dateRegistered = "dateRegistered",
            dateRegisteredGmt = "dateRegisteredGmt",
            email = "email",
            id = remoteId,
            name = "firstname lastname",
            ordersCount = 1,
            postcode = "postcode",
            state = "state",
            totalSpend = 0.0,
            userId = 1L
        )

        // WHEN
        val result = mapper.mapToModel(site, response)

        // THEN
        assertThat(result.isPayingCustomer).isEqualTo(false)
    }

    @Test
    fun `given customer name, then first name is properly assigned`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstname lastname")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("firstname", result.firstName)
    }

    @Test
    fun `given customer name as null, then first name returns empty string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = null)

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("", result.firstName)
    }

    @Test
    fun `given customer name with no space, then first name returns full string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstnamelastname")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("firstnamelastname", result.firstName)
    }

    @Test
    fun `given customer name, then last name is properly assigned`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstname lastname")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("lastname", result.lastName)
    }

    @Test
    fun `given customer name as null, then last name returns empty string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = null)

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("", result.lastName)
    }

    @Test
    fun `given customer name has no space, then last name returns empty string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstnamelastname")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("", result.lastName)
    }

    @Test
    fun `given customer name has multiple spaces, then first name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstname and a very long last name")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("firstname", result.firstName)
    }

    @Test
    fun `given customer name has multiple spaces, then last name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstname and a very long last name")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("and a very long last name", result.lastName)
    }

    @Test
    fun `given customer name has leading space, then first name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "  firstname and a very long last name")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("firstname", result.firstName)
    }

    @Test
    fun `given customer name has trailing space, then last name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "firstname and a very long last name  ")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("and a very long last name", result.lastName)
    }

    @Test
    fun `given customer name has multiple in between space, then first name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "  firstname   and  a   very  long last name")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("firstname", result.firstName)
    }

    @Test
    fun `given customer name has multiple in between space, then last name returns proper string`() {
        // GIVEN
        val siteId = 23
        val site = SiteModel().apply { id = siteId }

        val customerDTO = CustomerFromAnalyticsDTO(name = "  firstname   and  a   very  long last name")

        // WHEN
        val result = mapper.mapToModel(site, customerDTO)

        // THEN
        assertEquals("and  a   very  long last name", result.lastName)
    }
}
