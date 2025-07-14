package com.woocommerce.android.ui.moremenu.customer

import com.woocommerce.android.ui.orders.creation.customerlist.CustomerListRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerFromAnalytics
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult
import kotlin.test.Test

@ExperimentalCoroutinesApi
class RefreshCustomerDataTests : BaseUnitTest() {
    private val customerRepository: CustomerListRepository = mock()
    private val sut = RefreshCustomerData(customerRepository)

    @Test
    fun `when the customer have remoteCustomerId and analyticsCustomerId then refresh both values`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId = 2L

        // Mock the repository methods
        val customer: WCCustomerModel = mock()
        val analyticCustomer: WCCustomerFromAnalytics = mock()

        val customerResult = WooResult(customer)
        val analyticsResult = WooResult(analyticCustomer)

        whenever(customerRepository.fetchCustomerByRemoteId(remoteCustomerId)).thenReturn(customerResult)
        whenever(customerRepository.fetchCustomerFromAnalyticsByAnalyticsCustomerId(analyticsCustomerId)).thenReturn(
            analyticsResult
        )

        // Call the function
        sut(remoteCustomerId, analyticsCustomerId)

        // Verify the repository methods were called
        verify(customerRepository).fetchCustomerByRemoteId(remoteCustomerId)
        verify(customerRepository).fetchCustomerFromAnalyticsByAnalyticsCustomerId(analyticsCustomerId)
    }

    @Test
    fun `when the customer have remoteCustomerId and NOT analyticsCustomerId then refresh both values using remoteCustomerId`() = runTest {
        val remoteCustomerId = 1L
        val analyticsCustomerId: Long? = null

        // Mock the repository methods
        val customer: WCCustomerModel = mock()
        val analyticCustomer: WCCustomerFromAnalytics = mock()

        val customerResult = WooResult(customer)
        val analyticsResult = WooResult(analyticCustomer)

        whenever(customerRepository.fetchCustomerByRemoteId(remoteCustomerId)).thenReturn(customerResult)
        whenever(customerRepository.fetchCustomerFromAnalyticsByUserId(remoteCustomerId)).thenReturn(
            analyticsResult
        )

        // Call the function
        sut(remoteCustomerId, analyticsCustomerId)

        // Verify the repository methods were called
        verify(customerRepository).fetchCustomerByRemoteId(remoteCustomerId)
        verify(customerRepository).fetchCustomerFromAnalyticsByUserId(remoteCustomerId)
    }

    @Test
    fun `when the customer only have analyticsCustomerId then refresh analytics data`() = runTest {
        val remoteCustomerId = 0L
        val analyticsCustomerId = 2L

        // Mock the repository methods
        val analyticCustomer: WCCustomerFromAnalytics = mock()

        val analyticsResult = WooResult(analyticCustomer)

        whenever(
            customerRepository.fetchCustomerFromAnalyticsByAnalyticsCustomerId(analyticsCustomerId)
        ).thenReturn(analyticsResult)

        // Call the function
        sut(remoteCustomerId, analyticsCustomerId)

        // Verify the repository methods were called
        verify(customerRepository, never()).fetchCustomerByRemoteId(remoteCustomerId)
        verify(customerRepository).fetchCustomerFromAnalyticsByAnalyticsCustomerId(analyticsCustomerId)
    }

    @Test
    fun `when there is no valid ID then do nothing`() = runTest {
        val remoteCustomerId = 0L
        val analyticsCustomerId: Long? = null

        // Call the function
        sut(remoteCustomerId, analyticsCustomerId)

        // Verify the repository methods were called
        verify(customerRepository, never()).fetchCustomerByRemoteId(any())
        verify(customerRepository, never()).fetchCustomerFromAnalyticsByUserId(any())
        verify(customerRepository, never()).fetchCustomerFromAnalyticsByAnalyticsCustomerId(any())
    }
}
