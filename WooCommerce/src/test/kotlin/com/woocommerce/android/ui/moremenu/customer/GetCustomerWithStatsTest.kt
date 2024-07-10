package com.woocommerce.android.ui.moremenu.customer

import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.GetLocations
import com.woocommerce.android.model.Location
import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.DateUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.AssertionsForClassTypes.assertThat
import org.junit.Assert.assertNull
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalytics
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import kotlin.test.Test
import kotlin.test.assertNotNull

@OptIn(ExperimentalCoroutinesApi::class)
class GetCustomerWithStatsTest : BaseUnitTest() {
    private val customerRepository: CustomerListRepository = mock()
    private val currencyFormatter: CurrencyFormatter = mock()
    private val getLocations: GetLocations = mock()
    private val dateUtils: DateUtils = mock()

    private val defaultCustomer = WCCustomerModel().apply {
        avatarUrl = ""
        dateCreated = "2024-01-01"
        dateCreatedGmt = "2024-01-01"
        dateModified = "2024-01-01"
        dateModifiedGmt = "2024-01-01"
        email = "customer@email.com"
        firstName = "Test"
        remoteCustomerId = 1L
        isPayingCustomer = true
        lastName = "Customer"
        role = ""
        username = "customer"

        localSiteId = 1

        billingAddress1 = "default address"
        billingCity = "Customer City"
        billingCountry = "Customer Country"
        billingPostcode = "10000"
        billingState = "Customer State"

        shippingAddress1 = "default address"
        shippingCity = "Customer City"
        shippingCountry = "Customer Country"
        shippingPostcode = "10000"
        shippingState = "Customer State"
    }

    private val defaultCustomerFromAnalytics = WCCustomerFromAnalytics(
        avgOrderValue = 234.0,
        city = "Customer City",
        country = "Customer Country",
        dateLastActive = "2024-06-28",
        dateLastActiveGmt = "2024-06-28",
        dateLastOrder = "2024-06-28",
        dateRegistered = "2024-01-01",
        dateRegisteredGmt = "2024-01-01",
        email = "customer@email.com",
        id = 1L,
        name = "Test Customer",
        ordersCount = 200,
        postcode = "10000",
        state = "Customer State",
        totalSpend = 2347899.0,
        userId = 1L,
        username = "customer"
    )

    private val defaultCountryLocation = Location(code = "CU", name = "Customer Country")
    private val defaultStateLocation = AmbiguousLocation.Raw("CU")

    private val sut = GetCustomerWithStats(
        customerRepository = customerRepository,
        currencyFormatter = currencyFormatter,
        getLocations = getLocations,
        dateUtils = dateUtils
    )

    @Test
    fun `when invoke with both customer and analytics then merge data`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId = 2L

        whenever(customerRepository.getCustomerByRemoteId(remoteCustomerId)).thenReturn(defaultCustomer)
        whenever(customerRepository.getCustomerByAnalyticsCustomerId(analyticsCustomerId))
            .thenReturn(defaultCustomerFromAnalytics)

        whenever(getLocations.invoke(any(), any())).thenReturn(Pair(defaultCountryLocation, defaultStateLocation))

        whenever(dateUtils.getShortMonthDayAndYearStringFromFullIsoDate(anyString())).thenReturn("01-01-2024")

        val result = sut.invoke(remoteCustomerId, analyticsCustomerId)

        assert(result.isSuccess)
        val customerWithAnalytics = result.getOrNull()
        assertNotNull(customerWithAnalytics)

        // Assert customer is formed from both values
        assertThat(customerWithAnalytics.remoteCustomerId).isEqualTo(defaultCustomer.remoteCustomerId)
        assertThat(customerWithAnalytics.analyticsCustomerId).isEqualTo(defaultCustomerFromAnalytics.id)
        assertThat(customerWithAnalytics.lastActive).isNotEmpty()
    }

    @Test
    fun `when invoke without customer and with analytics then use analytics info`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId = 2L

        whenever(customerRepository.getCustomerByRemoteId(remoteCustomerId)).thenReturn(null)
        whenever(customerRepository.getCustomerByAnalyticsCustomerId(analyticsCustomerId))
            .thenReturn(defaultCustomerFromAnalytics)

        whenever(getLocations.invoke(any(), any())).thenReturn(Pair(defaultCountryLocation, defaultStateLocation))

        whenever(dateUtils.getShortMonthDayAndYearStringFromFullIsoDate(anyString())).thenReturn("01-01-2024")

        val result = sut.invoke(remoteCustomerId, analyticsCustomerId)

        assert(result.isSuccess)
        val customerWithAnalytics = result.getOrNull()
        assertNotNull(customerWithAnalytics)

        // Assert customer is formed from analytics
        assertThat(customerWithAnalytics.remoteCustomerId).isEqualTo(defaultCustomerFromAnalytics.userId)
        assertThat(customerWithAnalytics.analyticsCustomerId).isEqualTo(defaultCustomerFromAnalytics.id)
    }

    @Test
    fun `when invoke with customer and without analytics then use customer info`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId = 2L

        whenever(customerRepository.getCustomerByRemoteId(remoteCustomerId)).thenReturn(defaultCustomer)
        whenever(customerRepository.getCustomerByAnalyticsCustomerId(analyticsCustomerId))
            .thenReturn(null)

        whenever(getLocations.invoke(any(), any())).thenReturn(Pair(defaultCountryLocation, defaultStateLocation))

        whenever(dateUtils.getShortMonthDayAndYearStringFromFullIsoDate(anyString())).thenReturn("01-01-2024")

        val result = sut.invoke(remoteCustomerId, analyticsCustomerId)

        assert(result.isSuccess)
        val customerWithAnalytics = result.getOrNull()
        assertNotNull(customerWithAnalytics)

        // Assert customer is formed from customer info
        assertThat(customerWithAnalytics.remoteCustomerId).isEqualTo(defaultCustomer.remoteCustomerId)
        assertThat(customerWithAnalytics.analyticsCustomerId).isEqualTo(null)
    }

    @Test
    fun `when invoke without customer and without analytics then fails`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId = 2L

        whenever(customerRepository.getCustomerByRemoteId(remoteCustomerId)).thenReturn(null)
        whenever(customerRepository.getCustomerByAnalyticsCustomerId(analyticsCustomerId)).thenReturn(null)

        val result = sut.invoke(remoteCustomerId, analyticsCustomerId)

        assert(result.isFailure)
        val customerWithAnalytics = result.getOrNull()
        assertNull(customerWithAnalytics)
    }
}
