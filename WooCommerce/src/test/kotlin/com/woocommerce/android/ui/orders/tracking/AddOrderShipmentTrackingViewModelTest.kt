package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.R
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.tools.NetworkStatus
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.tracking.AddOrderShipmentTrackingViewModel.SaveTrackingPrefsEvent
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.store.WCOrderStore.OnOrderChanged
import org.wordpress.android.fluxc.store.WCOrderStore.OrderError
import org.wordpress.android.fluxc.store.WCOrderStore.OrderErrorType.GENERIC_ERROR
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AddOrderShipmentTrackingViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_IDENTIFIER = "1-1-1"
    }

    private val networkStatus: NetworkStatus = mock()
    private val repository: OrderDetailRepository = mock()

    private val savedState = AddOrderShipmentTrackingFragmentArgs(orderId = ORDER_IDENTIFIER).initSavedStateHandle()

    private lateinit var viewModel: AddOrderShipmentTrackingViewModel

    @Before
    fun setup() {
        viewModel = AddOrderShipmentTrackingViewModel(
            savedState = savedState,
            networkStatus = networkStatus,
            orderDetailRepository = repository
        )
        doReturn(true).whenever(networkStatus).isConnected()
    }

    @Test
    fun `Add order shipment tracking when network is available - success`() = runBlockingTest {
        doReturn(OnOrderChanged(0)).whenever(repository).addOrderShipmentTracking(any(), any())

        val events = mutableListOf<Event>()
        viewModel.event.observeForever { events.add(it) }
        viewModel.onCarrierSelected(Carrier("test", false))
        viewModel.onTrackingNumberEntered("123456")
        viewModel.onAddButtonTapped()

        verify(repository, times(1))
            .addOrderShipmentTracking(
                orderIdentifier = eq(ORDER_IDENTIFIER),
                shipmentTrackingModel = argThat {
                    trackingProvider == "test" &&
                        trackingNumber == "123456" && !isCustomProvider
                }
            )

        assertThat(events[0]).isInstanceOf(SaveTrackingPrefsEvent::class.java)
        assertEquals(R.string.order_shipment_tracking_added, (events[1] as ShowSnackbar).message)
        assertThat(events[2]).isInstanceOf(ExitWithResult::class.java)
    }

    @Test
    fun `Add order shipment tracking fails`() = runBlockingTest {
        doReturn(OnOrderChanged(0).also { it.error = OrderError(type = GENERIC_ERROR, message = "") })
            .whenever(repository).addOrderShipmentTracking(any(), any())

        val events = mutableListOf<Event>()
        viewModel.event.observeForever { events.add(it) }
        viewModel.onCarrierSelected(Carrier("test", false))
        viewModel.onTrackingNumberEntered("123456")
        viewModel.onAddButtonTapped()

        verify(repository, times(1))
            .addOrderShipmentTracking(
                orderIdentifier = eq(ORDER_IDENTIFIER),
                shipmentTrackingModel = argThat {
                    trackingProvider == "test" &&
                        trackingNumber == "123456" && !isCustomProvider
                }
            )

        assertThat(events[0]).isInstanceOf(SaveTrackingPrefsEvent::class.java)
        assertEquals(R.string.order_shipment_tracking_error, (events[1] as ShowSnackbar).message)
    }

    @Test
    fun `Add order shipment tracking when network offline`() = runBlockingTest {
        doReturn(false).whenever(networkStatus).isConnected()

        var event: Event? = null
        viewModel.event.observeForever { event = it }
        viewModel.onCarrierSelected(Carrier("test", false))
        viewModel.onTrackingNumberEntered("123456")
        viewModel.onAddButtonTapped()

        verify(repository, times(0)).addOrderShipmentTracking(any(), any())
        assertEquals(R.string.offline_error, (event as ShowSnackbar).message)
    }
}
