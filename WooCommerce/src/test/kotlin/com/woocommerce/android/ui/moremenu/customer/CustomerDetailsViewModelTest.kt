package com.woocommerce.android.ui.moremenu.customer

import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerDetailsViewModelTest : BaseUnitTest() {
    private val refreshCustomerData: RefreshCustomerData = mock()
    private val getCustomerWithStats: GetCustomerWithStats = mock()

    private lateinit var sut: CustomerDetailsViewModel

    private val navCustomer = CustomerWithAnalytics(
        remoteCustomerId = 1L,
        analyticsCustomerId = 1L,
        firstName = "Customer",
        lastName = "Test",
        username = "username",
        email = "customer@email.com",
        phone = "",
        lastActive = null,
        ordersCount = null,
        totalSpend = null,
        averageOrderValue = null,
        registeredDate = "2024-06-28",
        billingAddress = Address.EMPTY,
        shippingAddress = Address.EMPTY
    )

    private val defaultCustomer = CustomerWithAnalytics(
        remoteCustomerId = 1L,
        analyticsCustomerId = 1L,
        firstName = "Customer",
        lastName = "Test",
        username = "username",
        email = "customer@email.com",
        phone = "",
        lastActive = "2024-06-28",
        ordersCount = 123,
        totalSpend = "$234,000.00",
        averageOrderValue = "$78.00",
        registeredDate = "2024-06-28",
        billingAddress = Address.EMPTY,
        shippingAddress = Address.EMPTY
    )

    private fun createViewModel() {
        val navArgs = CustomerDetailsFragmentArgs(navCustomer)
        sut = CustomerDetailsViewModel(navArgs.toSavedStateHandle(), refreshCustomerData, getCustomerWithStats)
    }

    @Test
    fun `when the view start then refresh the data`() = runTest {
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))

        createViewModel()
        verify(refreshCustomerData).invoke(any(), any())
        verify(getCustomerWithStats).invoke(any(), any())
    }

    @Test
    fun `when the view start then present the updated customer`() = runTest {
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))
        createViewModel()

        val state = sut.viewState.captureValues().last()

        assertThat(state.customerWithAnalytics).isEqualTo(defaultCustomer)
        assertThat(state.isLoadingAnalytics).isFalse()
    }

    @Test
    fun `when refresh is called then refresh the data`() = runTest {
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))
        createViewModel()
        clearInvocations(refreshCustomerData)
        clearInvocations(getCustomerWithStats)

        sut.refresh()

        verify(refreshCustomerData).invoke(any(), any())
        verify(getCustomerWithStats).invoke(any(), any())
    }
}
