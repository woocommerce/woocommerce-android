package com.woocommerce.android.ui.moremenu.customer

import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.CustomerWithAnalytics
import com.woocommerce.android.util.captureValues
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
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
    private val resourceProvider: ResourceProvider = mock()

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
        sut = CustomerDetailsViewModel(
            navArgs.toSavedStateHandle(),
            refreshCustomerData,
            getCustomerWithStats,
            resourceProvider
        )
    }

    @Test
    fun `when the view start then refresh the data`() = runTest {
        // GIVEN
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))

        // WHEN
        createViewModel()

        // THEN
        verify(refreshCustomerData).invoke(any(), any())
        verify(getCustomerWithStats).invoke(any(), any())
    }

    @Test
    fun `when the view start then present the updated customer`() = runTest {
        // GIVEN
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))
        createViewModel()

        // WHEN
        val state = sut.viewState.captureValues().last()

        // THEN
        assertThat(state.customerWithAnalytics).isEqualTo(defaultCustomer)
        assertThat(state.isLoadingAnalytics).isFalse()
    }

    @Test
    fun `when refresh is called then refresh the data`() = runTest {
        // GIVEN
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(defaultCustomer))
        createViewModel()
        clearInvocations(refreshCustomerData)
        clearInvocations(getCustomerWithStats)

        // WHEN
        sut.refresh()

        // THEN
        verify(refreshCustomerData).invoke(any(), any())
        verify(getCustomerWithStats).invoke(any(), any())
    }

    @Test
    fun `when customer has non-blank name then customerName contains full name`() = runTest {
        // GIVEN
        val customerWithName = defaultCustomer.copy(firstName = "John", lastName = "Doe")
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(customerWithName))
        whenever(resourceProvider.getString(R.string.customer_detail_guest_customer)).doReturn("Guest")

        // WHEN
        createViewModel()

        // THEN
        val state = sut.viewState.captureValues().last()
        assertThat(state.customerName).isEqualTo("John Doe")
    }

    @Test
    fun `when customer has blank name then customerName shows guest label`() = runTest {
        // GIVEN
        val customerWithBlankName = defaultCustomer.copy(firstName = "", lastName = "")
        val guestString = "Guest Customer"
        whenever(getCustomerWithStats.invoke(any(), any())).doReturn(Result.success(customerWithBlankName))
        whenever(resourceProvider.getString(R.string.customer_detail_guest_customer)).doReturn(guestString)

        // WHEN
        createViewModel()

        // THEN
        val state = sut.viewState.captureValues().last()
        assertThat(state.customerName).isEqualTo(guestString)
    }
}
