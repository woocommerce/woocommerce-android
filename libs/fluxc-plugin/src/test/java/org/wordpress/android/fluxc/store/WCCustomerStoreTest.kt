package org.wordpress.android.fluxc.store

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalyticsMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerMapper
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.BaseRequest.GenericErrorType.NETWORK_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType.INVALID_RESPONSE
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooPayload
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.CustomerRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.wc.customer.dto.CustomerDTO
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.CustomerDao
import org.wordpress.android.fluxc.test
import org.wordpress.android.fluxc.tools.initCoroutineEngine

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCCustomerStoreTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private val restClient: CustomerRestClient = mock()

    private lateinit var customerDao: CustomerDao
    private lateinit var sut: WCCustomerStore

    @Before
    fun setUp() {
        customerDao = databaseRule.db.customerDao

        sut = WCCustomerStore(
            restClient,
            initCoroutineEngine(),
            WCCustomerMapper(),
            customerDao,
            databaseRule.db.customerFromAnalyticsDao,
            WCCustomerFromAnalyticsMapper()
        )
    }

    @Test
    fun `fetchSingleCustomer with success returns customer model`() = runTest {
        // given
        val customerDto = fakeCustomerDTO(id = DEFAULT_REMOTE_CUSTOMER_ID)
        val expectedCustomer = fakeWCCustomerModel(
            remoteCustomerId = DEFAULT_REMOTE_CUSTOMER_ID,
            localSiteId = DEFAULT_SITE_ID
        )

        doReturn(WooPayload(customerDto))
            .whenever(restClient)
            .fetchSingleCustomer(DEFAULT_SITE_MODEL, DEFAULT_REMOTE_CUSTOMER_ID)

        // when
        val result = sut.fetchSingleCustomer(DEFAULT_SITE_MODEL, DEFAULT_REMOTE_CUSTOMER_ID)

        // then
        assertThat(result.isError).isFalse
        assertThat(result.model).isEqualTo(expectedCustomer)
    }

    @Test
    fun `fetchSingleCustomer with error returns error result`() = runTest {
        // given
        doReturn(WooPayload<WCCustomerModel>(TEST_ERROR))
            .whenever(restClient)
            .fetchSingleCustomer(DEFAULT_SITE_MODEL, DEFAULT_REMOTE_CUSTOMER_ID)

        // when
        val result = sut.fetchSingleCustomer(DEFAULT_SITE_MODEL, DEFAULT_REMOTE_CUSTOMER_ID)

        // then
        assertThat(result.isError).isTrue
        assertThat(result.error).isEqualTo(TEST_ERROR)
    }

    @Test
    fun `fetchCustomers with success returns mapped customer models and caches them`() = runTest {
        // given
        val customerDto = fakeCustomerDTO(id = DEFAULT_REMOTE_CUSTOMER_ID)
        val expectedCustomer = fakeWCCustomerModel(
            remoteCustomerId = DEFAULT_REMOTE_CUSTOMER_ID,
            localSiteId = DEFAULT_SITE_ID
        )

        doReturn(WooPayload(arrayOf(customerDto)))
            .whenever(restClient)
            .fetchCustomers(
                site = DEFAULT_SITE_MODEL,
                search = "jo",
                email = "customer@example.com",
                include = listOf(DEFAULT_REMOTE_CUSTOMER_ID),
                orderby = "email",
                order = "asc",
                page = 2,
                perPage = 50,
            )

        // when
        val result = sut.fetchCustomers(
            site = DEFAULT_SITE_MODEL,
            search = "jo",
            email = "customer@example.com",
            include = listOf(DEFAULT_REMOTE_CUSTOMER_ID),
            orderby = "email",
            order = "asc",
            page = 2,
            perPage = 50,
        )

        // then
        assertThat(result.isError).isFalse
        assertThat(result.model).containsExactly(expectedCustomer)
        assertThat(customerDao.getCustomersForSite(DEFAULT_SITE_MODEL.localId())).containsExactly(expectedCustomer)
    }

    @Test
    fun `fetchCustomers with error returns error and does not cache`() = runTest {
        // given
        doReturn(WooPayload<Array<CustomerDTO>>(TEST_ERROR))
            .whenever(restClient)
            .fetchCustomers(DEFAULT_SITE_MODEL)

        // when
        val result = sut.fetchCustomers(DEFAULT_SITE_MODEL)

        // then
        assertThat(result.isError).isTrue
        assertThat(result.error).isEqualTo(TEST_ERROR)
        assertThat(customerDao.getCustomersForSite(DEFAULT_SITE_MODEL.localId())).isEmpty()
    }

    @Test
    fun `fetchCustomersFromAnalytics with error returns error and does not cache`() = test {
        // given
        whenever(
            restClient.fetchCustomersFromAnalytics(
                DEFAULT_SITE_MODEL,
                page = 1,
                pageSize = DEFAULT_PAGE_SIZE
            )
        ).thenReturn(WooPayload(TEST_ERROR))

        // when
        val result = sut.fetchCustomersFromAnalytics(DEFAULT_SITE_MODEL, 1)

        // then
        assertThat(result.isError).isTrue
        assertThat(customerDao.getCustomersForSite(DEFAULT_SITE_MODEL.localId())).isEmpty()
    }

    private companion object {
        const val DEFAULT_SITE_ID = 1
        const val DEFAULT_REMOTE_CUSTOMER_ID = 2L
        const val DEFAULT_PAGE_SIZE = 25

        val DEFAULT_SITE_MODEL = SiteModel().apply { id = DEFAULT_SITE_ID }
        val TEST_ERROR = WooError(INVALID_RESPONSE, NETWORK_ERROR, "Invalid site ID")

        fun fakeCustomerDTO(
            id: Long = 1L,
            email: String = "customer@example.com",
            firstName: String = "John",
            lastName: String = "Doe",
            username: String = "johndoe",
            avatarUrl: String = "https://example.com/avatar.jpg",
            dateCreated: String = "2023-01-01T00:00:00",
            dateCreatedGmt: String = "2023-01-01T00:00:00",
            dateModified: String = "2023-01-01T00:00:00",
            dateModifiedGmt: String = "2023-01-01T00:00:00",
            isPayingCustomer: Boolean = false,
            role: String = "customer",
            billing: CustomerDTO.Billing? = null,
            shipping: CustomerDTO.Shipping? = null
        ): CustomerDTO {
            return CustomerDTO(
                id = id,
                email = email,
                firstName = firstName,
                lastName = lastName,
                username = username,
                avatarUrl = avatarUrl,
                dateCreated = dateCreated,
                dateCreatedGmt = dateCreatedGmt,
                dateModified = dateModified,
                dateModifiedGmt = dateModifiedGmt,
                isPayingCustomer = isPayingCustomer,
                role = role,
                billing = billing ?: CustomerDTO.Billing(
                    address1 = "123 Main St",
                    address2 = "Apt 4B",
                    city = "Anytown",
                    company = "ACME Inc",
                    country = "US",
                    email = email,
                    firstName = firstName,
                    lastName = lastName,
                    phone = "555-1234",
                    postcode = "12345",
                    state = "CA"
                ),
                shipping = shipping ?: CustomerDTO.Shipping(
                    address1 = "123 Main St",
                    address2 = "Apt 4B",
                    city = "Anytown",
                    company = "ACME Inc",
                    country = "US",
                    firstName = firstName,
                    lastName = lastName,
                    postcode = "12345",
                    state = "CA"
                )
            )
        }

        fun fakeWCCustomerModel(
            remoteCustomerId: Long = 1L,
            localSiteId: Int = DEFAULT_SITE_ID,
            email: String = "customer@example.com",
            firstName: String = "John",
            lastName: String = "Doe",
            username: String = "johndoe",
            avatarUrl: String = "https://example.com/avatar.jpg",
            dateCreated: String = "2023-01-01T00:00:00",
            dateCreatedGmt: String = "2023-01-01T00:00:00",
            dateModified: String = "2023-01-01T00:00:00",
            dateModifiedGmt: String = "2023-01-01T00:00:00",
            isPayingCustomer: Boolean = false,
            role: String = "customer"
        ): WCCustomerModel {
            return WCCustomerModel(
                remoteCustomerId = RemoteId(remoteCustomerId),
                localSiteId = LocalId(localSiteId),
                email = email,
                firstName = firstName,
                lastName = lastName,
                username = username,
                avatarUrl = avatarUrl,
                dateCreated = dateCreated,
                dateCreatedGmt = dateCreatedGmt,
                dateModified = dateModified,
                dateModifiedGmt = dateModifiedGmt,
                isPayingCustomer = isPayingCustomer,
                role = role,
                billingAddress1 = "123 Main St",
                billingAddress2 = "Apt 4B",
                billingCity = "Anytown",
                billingCompany = "ACME Inc",
                billingCountry = "US",
                billingEmail = email,
                billingFirstName = firstName,
                billingLastName = lastName,
                billingPhone = "555-1234",
                billingPostcode = "12345",
                billingState = "CA",
                shippingAddress1 = "123 Main St",
                shippingAddress2 = "Apt 4B",
                shippingCity = "Anytown",
                shippingCompany = "ACME Inc",
                shippingCountry = "US",
                shippingFirstName = firstName,
                shippingLastName = lastName,
                shippingPostcode = "12345",
                shippingState = "CA",
            )
        }
    }
}
