package com.woocommerce.android.ui.orders.creation.customerlist

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Location
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.customer.WCCustomerModel
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooResult

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class CustomerListViewModelTest : BaseUnitTest() {
    lateinit var viewModel: CustomerListViewModel

    private val savedState: SavedStateHandle = mock()
    private val networkStatus: NetworkStatus = mock()
    private val customerListRepository: CustomerListRepository = mock()

    @Before
    fun setup() {
        viewModel = CustomerListViewModel(savedState, networkStatus, customerListRepository)
    }

    @Test
    fun `given no internet, when searching customers, then show offline error`() = testBlocking {
        // GIVEN
        val query = "test"
        whenever(networkStatus.isConnected()).thenReturn(false)

        // WHEN
        viewModel.onSearchQueryChanged(query)
        advanceUntilIdle()

        // THEN
        val event = viewModel.event.value as MultiLiveEvent.Event.ShowSnackbar
        assertThat(event.message).isEqualTo(R.string.offline_error)
    }

    @Test
    fun `given internet, when search customers, then fetching customers`() = testBlocking {
        // GIVEN
        val query = "test"
        whenever(networkStatus.isConnected()).thenReturn(true)

        // WHEN
        viewModel.onSearchQueryChanged(query)
        advanceUntilIdle()

        // THEN
        verify(customerListRepository).searchCustomerList(query)
    }

    @Test
    fun `given search customer list success, when search customer, then list shown`() = testBlocking {
        // GIVEN
        whenever(networkStatus.isConnected()).thenReturn(true)
        val query = "test"
        val customer: WCCustomerModel = mock {
            on { remoteCustomerId }.thenReturn(1L)
            on { firstName }.thenReturn("firstName")
            on { lastName }.thenReturn("lastName")
            on { email }.thenReturn("email")
            on { avatarUrl }.thenReturn("avatarUrl")
        }
        val customerList = listOf(customer)
        whenever(customerListRepository.searchCustomerList(query)).thenReturn(customerList)

        // WHEN
        viewModel.onSearchQueryChanged(query)
        advanceUntilIdle()

        // THEN
        val viewState = viewModel.viewState.value
        assertThat(viewState!!.customers).isEqualTo(
            listOf(
                CustomerListViewModel.CustomerListItem(
                    remoteId = 1L,
                    firstName = "firstName",
                    lastName = "lastName",
                    email = "email",
                    avatarUrl = "avatarUrl"
                )
            )
        )
    }

    @Test
    fun `given customer with user id 0, when on customer clicked, then local customer used`() =
        testBlocking {
            // GIVEN
            val customerId = 0L
            val customer = createCustomer(customerId)
            whenever(customerListRepository.getCustomerByRemoteIdFromLocalStorage(customerId)).thenReturn(customer)
            val country = Location(
                code = "code",
                name = "name",
                parentCode = "parentCode",
            )
            val state = Location(
                code = "code1",
                name = "name1",
                parentCode = "parentCode1",
            )
            whenever(customerListRepository.getCountry("us")).thenReturn(country)
            whenever(customerListRepository.getState("us", "ny")).thenReturn(state)

            // WHEN
            viewModel.onCustomerClick(customerId)

            // THEN
            val event = viewModel.event.value as CustomerListViewModel.CustomerSelected
            assertThat(event.customerId).isEqualTo(customerId)
            assertThat(event.billingAddress.lastName).isEqualTo(customer.billingLastName)
            assertThat(event.billingAddress.firstName).isEqualTo(customer.billingFirstName)
            assertThat(event.billingAddress.company).isEqualTo(customer.billingCompany)
            assertThat(event.billingAddress.email).isEqualTo(customer.billingEmail)
            assertThat(event.billingAddress.phone).isEqualTo(customer.billingPhone)
            assertThat(event.billingAddress.country).isEqualTo(country)
            assertThat(event.billingAddress.postcode).isEqualTo(customer.billingPostcode)

            assertThat(event.shippingAddress.postcode).isEqualTo(customer.shippingPostcode)
            assertThat(event.shippingAddress.lastName).isEqualTo(customer.shippingLastName)
            assertThat(event.shippingAddress.firstName).isEqualTo(customer.shippingFirstName)
            assertThat(event.shippingAddress.company).isEqualTo(customer.shippingCompany)
            assertThat(event.shippingAddress.email).isEqualTo("")
            assertThat(event.shippingAddress.phone).isEqualTo("")
            assertThat(event.shippingAddress.country).isEqualTo(country)
            assertThat(event.shippingAddress.postcode).isEqualTo(customer.shippingPostcode)
        }

    @Test
    fun `given customer with user id not 0, when on customer clicked, then customer fetched remotely used`() =
        testBlocking {
            // GIVEN
            val customerId = 1L
            val customer = createCustomer(customerId)
            whenever(customerListRepository.fetchCustomerByRemoteId(customerId)).thenReturn(WooResult(customer))
            val country = Location(
                code = "code",
                name = "name",
                parentCode = "parentCode",
            )
            val state = Location(
                code = "code1",
                name = "name1",
                parentCode = "parentCode1",
            )
            whenever(customerListRepository.getCountry("us")).thenReturn(country)
            whenever(customerListRepository.getState("us", "ny")).thenReturn(state)

            // WHEN
            viewModel.onCustomerClick(customerId)

            // THEN
            val event = viewModel.event.value as CustomerListViewModel.CustomerSelected
            assertThat(event.customerId).isEqualTo(customerId)
            assertThat(event.billingAddress.lastName).isEqualTo(customer.billingLastName)
            assertThat(event.billingAddress.firstName).isEqualTo(customer.billingFirstName)
            assertThat(event.billingAddress.company).isEqualTo(customer.billingCompany)
            assertThat(event.billingAddress.email).isEqualTo(customer.billingEmail)
            assertThat(event.billingAddress.phone).isEqualTo(customer.billingPhone)
            assertThat(event.billingAddress.country).isEqualTo(country)
            assertThat(event.billingAddress.postcode).isEqualTo(customer.billingPostcode)

            assertThat(event.shippingAddress.postcode).isEqualTo(customer.shippingPostcode)
            assertThat(event.shippingAddress.lastName).isEqualTo(customer.shippingLastName)
            assertThat(event.shippingAddress.firstName).isEqualTo(customer.shippingFirstName)
            assertThat(event.shippingAddress.company).isEqualTo(customer.shippingCompany)
            assertThat(event.shippingAddress.email).isEqualTo("")
            assertThat(event.shippingAddress.phone).isEqualTo("")
            assertThat(event.shippingAddress.country).isEqualTo(country)
            assertThat(event.shippingAddress.postcode).isEqualTo(customer.shippingPostcode)
        }

    private fun createCustomer(customerId: Long): WCCustomerModel {
        val customer: WCCustomerModel = mock {
            on { remoteCustomerId }.thenReturn(customerId)
            on { shippingCompany }.thenReturn("shippingCompany")
            on { shippingAddress1 }.thenReturn("shippingAddress1")
            on { shippingAddress2 }.thenReturn("shippingAddress2")
            on { billingAddress1 }.thenReturn("billingAddress1")
            on { billingAddress2 }.thenReturn("billingAddress2")
            on { billingCity }.thenReturn("billingCity")
            on { shippingCity }.thenReturn("shippingCity")
            on { billingPostcode }.thenReturn("billingPostcode")
            on { shippingPostcode }.thenReturn("shippingPostcode")
            on { billingCountry }.thenReturn("us")
            on { shippingCountry }.thenReturn("us")
            on { billingState }.thenReturn("ny")
            on { shippingState }.thenReturn("ny")
            on { shippingFirstName }.thenReturn("shippingFirstName")
            on { shippingLastName }.thenReturn("shippingLastName")
            on { billingFirstName }.thenReturn("billingFirstName")
            on { billingLastName }.thenReturn("billingLastName")
            on { billingCompany }.thenReturn("billingCompany")
            on { billingPhone }.thenReturn("billingPhone")
            on { billingEmail }.thenReturn("billingEmail")
        }
        return customer
    }
}
