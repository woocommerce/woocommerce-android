package com.woocommerce.android.ui.orders.details.editing

import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.details.editing.address.testCountry
import com.woocommerce.android.ui.orders.details.editing.address.testState
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.store.WCOrderStore.*
import org.wordpress.android.fluxc.store.WCOrderStore.UpdateOrderResult.RemoteUpdateResult

@ExperimentalCoroutinesApi
class OrderEditingViewModelTest : BaseUnitTest() {
    private lateinit var sut: OrderEditingViewModel

    private val orderEditingRepository: OrderEditingRepository = mock()
    private val orderDetailRepository: OrderDetailRepository = mock {
        onBlocking { getOrderById(any()) } doReturn testOrder
    }
    private val networkStatus: NetworkStatus = mock {
        on { isConnected() } doReturn true
    }

    @Before
    fun setUp() {
        sut = OrderEditingViewModel(
            SavedStateHandle().apply { set("orderId", 1L) },
            coroutinesTestRule.testDispatchers,
            orderDetailRepository,
            orderEditingRepository,
            networkStatus
        )
    }

    @Test
    fun `should replicate billing to shipping when toggle is activated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            orderEditingRepository.stub {
                onBlocking {
                    updateBothOrderAddresses(any(), any(), any())
                } doReturn flowOf()
            }

            sut.apply {
                start()
                onReplicateAddressSwitchChanged(true)
                updateBillingAddress(addressToUpdate)
            }

            verify(orderEditingRepository)
                .updateBothOrderAddresses(
                    testOrder.id,
                    addressToUpdate.toShippingAddressModel(),
                    addressToUpdate.toBillingAddressModel("")
                )
        }

    @Test
    fun `should replicate shipping to billing when toggle is activated`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            orderEditingRepository.stub {
                onBlocking {
                    updateBothOrderAddresses(any(), any(), any())
                } doReturn flowOf()
            }

            sut.apply {
                start()
                onReplicateAddressSwitchChanged(true)
                updateShippingAddress(addressToUpdate)
            }

            verify(orderEditingRepository)
                .updateBothOrderAddresses(
                    testOrder.id,
                    addressToUpdate.toShippingAddressModel(),
                    addressToUpdate.toBillingAddressModel("")
                )
        }

    @Test
    fun `should NOT replicate shipping to billing when toggle is deactivated`() {
        var eventWasCalled = false

        sut.apply {
            start()
            onReplicateAddressSwitchChanged(false)
            updateShippingAddress(addressToUpdate)
        }

        observeEvents {
            eventWasCalled = when (it) {
                is OrderEditingViewModel.OrderEdited -> true
                else -> false
            }
        }

        assertThat(eventWasCalled).isFalse
    }

    @Test
    fun `should NOT replicate billing to shipping when toggle is deactivated`() {
        var eventWasCalled = false

        sut.apply {
            start()
            onReplicateAddressSwitchChanged(false)
            updateBillingAddress(addressToUpdate)
        }

        observeEvents {
            eventWasCalled = when (it) {
                is OrderEditingViewModel.OrderEdited -> true
                else -> false
            }
        }

        assertThat(eventWasCalled).isFalse
    }

    @Test
    fun `should replace email info with original one when empty`() =
        coroutinesTestRule.testDispatcher.runBlockingTest {
            val originalOrder = testOrder.copy(
                billingAddress = addressToUpdate.copy(email = "original@email.com")
            )

            orderDetailRepository.stub {
                onBlocking { getOrderById(any()) } doReturn originalOrder
            }

            orderEditingRepository.stub {
                onBlocking {
                    updateBothOrderAddresses(any(), any(), any())
                } doReturn flowOf()
            }

            sut.apply {
                start()
                onReplicateAddressSwitchChanged(true)
                updateBillingAddress(addressToUpdate)
            }

            verify(orderEditingRepository)
                .updateBothOrderAddresses(
                    testOrder.id,
                    addressToUpdate.toShippingAddressModel(),
                    addressToUpdate.toBillingAddressModel("original@email.com")
                )
        }

    @Test
    fun `should execute updateBillingAddress only when connection is available`() {
        var eventWasCalled = false
        networkStatus.stub {
            on { isConnected() } doReturn false
        }

        sut.apply {
            start()
            updateBillingAddress(addressToUpdate)
        }

        observeEvents {
            eventWasCalled = when (it) {
                is OrderEditingViewModel.OrderEdited -> true
                else -> false
            }
        }

        assertThat(eventWasCalled).isFalse
    }

    @Test
    fun `should execute updateShippingAddress only when connection is available`() {
        var eventWasCalled = false
        networkStatus.stub {
            on { isConnected() } doReturn false
        }

        sut.apply {
            start()
            updateShippingAddress(addressToUpdate)
        }

        observeEvents {
            eventWasCalled = when (it) {
                is OrderEditingViewModel.OrderEdited -> true
                else -> false
            }
        }

        assertThat(eventWasCalled).isFalse
    }

    @Test
    fun `should execute updateCustomerOrderNote only when connection is available`() {
        var eventWasCalled = false
        networkStatus.stub {
            on { isConnected() } doReturn false
        }

        sut.apply {
            start()
            updateCustomerOrderNote("test note")
        }

        observeEvents {
            eventWasCalled = when (it) {
                is OrderEditingViewModel.OrderEdited -> true
                else -> false
            }
        }

        assertThat(eventWasCalled).isFalse
    }

    @Test
    fun `should emit success event if update was successful`() {
        var eventWasCalled = false
        orderEditingRepository.stub {
            onBlocking {
                updateOrderAddress(testOrder.id, addressToUpdate.toBillingAddressModel())
            } doReturn flowOf(
                UpdateOrderResult.OptimisticUpdateResult(
                    OnOrderChanged()
                )
            )
        }

        sut.apply {
            start()
            updateBillingAddress(addressToUpdate)
        }

        observeEvents { event ->
            eventWasCalled = true
            assertThat(event).isEqualTo(OrderEditingViewModel.OrderEdited)
        }

        assertThat(eventWasCalled).isTrue
    }

    @Test
    fun `should emit generic error event for errors other than empty billing mail error`() {
        orderEditingRepository.stub {
            onBlocking {
                updateOrderAddress(testOrder.id, addressToUpdate.toBillingAddressModel())
            } doReturn flowOf(
                RemoteUpdateResult(
                    OnOrderChanged(orderError = OrderError(type = OrderErrorType.INVALID_RESPONSE))
                )
            )
        }

        sut.apply {
            start()
            updateBillingAddress(addressToUpdate)
        }

        observeEvents { event ->
            assertThat(event).isEqualTo(OrderEditingViewModel.OrderEditFailed(R.string.order_error_update_general))
        }
    }

    @Test
    fun `should emit empty mail failure if store returns empty billing mail error`() {
        orderEditingRepository.stub {
            onBlocking {
                updateOrderAddress(testOrder.id, addressToUpdate.toBillingAddressModel())
            } doReturn flowOf(
                RemoteUpdateResult(
                    OnOrderChanged(orderError = OrderError(type = OrderErrorType.EMPTY_BILLING_EMAIL))
                )
            )
        }

        sut.apply {
            start()
            updateBillingAddress(addressToUpdate)
        }

        observeEvents { event ->
            assertThat(event).isEqualTo(OrderEditingViewModel.OrderEditFailed(R.string.order_error_update_empty_mail))
        }
    }

    private fun observeEvents(check: (MultiLiveEvent.Event) -> Unit) =
        sut.event.observeForever { check(it) }

    private companion object {
        val addressToUpdate = Address(
            company = "Automattic",
            firstName = "Joe",
            lastName = "Doe",
            phone = "123456789",
            country = testCountry,
            state = testState,
            address1 = "Address 1",
            address2 = "",
            city = "San Francisco",
            postcode = "12345",
            email = ""
        )

        val testOrder = OrderTestUtils.generateTestOrder()
    }
}
