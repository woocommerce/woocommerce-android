package com.woocommerce.android.ui.orders.tracking

import com.woocommerce.android.R
import com.woocommerce.android.model.OrderShipmentProvider
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.orders.tracking.AddOrderTrackingProviderListViewModel.ViewState
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchException
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult
import com.woocommerce.android.ui.orders.tracking.OrderShipmentProvidersRepository.OrderShipmentProvidersFetchResult.NoCarriersFound
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ExitWithResult
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class AddOrderTrackingProviderListViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val orderDetailRepository: OrderDetailRepository = mock()
    private val shipmentProvidersRepository: OrderShipmentProvidersRepository = mock()
    private val resourceProvider: ResourceProvider = mock()

    private val testShipmentProvider = listOf(
        OrderShipmentProvider(carrierName = "test aaa", carrierLink = "", country = "US"),
        OrderShipmentProvider(carrierName = "test bbb", carrierLink = "", country = "US"),
        OrderShipmentProvider(carrierName = "test ccc", carrierLink = "", country = "US")
    )

    private lateinit var viewModel: AddOrderTrackingProviderListViewModel

    private val savedState = AddOrderTrackingProviderListFragmentArgs(orderId = ORDER_ID).toSavedStateHandle()

    fun setupViewModel() {
        viewModel = AddOrderTrackingProviderListViewModel(
            savedState = savedState,
            orderDetailRepository = orderDetailRepository,
            shipmentProvidersRepository = shipmentProvidersRepository,
            resourceProvider = resourceProvider
        )
    }

    @Test
    fun `Shows and hides the provider list skeleton correctly`() = runTest {
        doReturn(flowOf(testShipmentProvider)).whenever(shipmentProvidersRepository)
            .observeOrderShipmentProviders()

        setupViewModel()
        var state: ViewState? = null
        viewModel.trackingProviderListViewStateData.observeForever { _, viewState ->
            state = viewState
        }

        verify(shipmentProvidersRepository, times(1)).fetchOrderShipmentProviders(ORDER_ID)
        assertThat(state!!.showSkeleton).isFalse()
        assertThat(state!!.providersList).isEqualTo(testShipmentProvider)
    }

    @Test
    fun `Display error snackbar when no carriers has been found on remote`() = runTest {
        doReturn(Result.success(NoCarriersFound))
            .whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_ID)

        setupViewModel()
        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertThat((event as ShowSnackbar).message)
            .isEqualTo(R.string.order_shipment_tracking_provider_list_error_empty_list)
    }

    @Test
    fun `Display error snackbar when error occurs`() = testBlocking {
        doReturn(
            Result.failure<OrderShipmentProvidersFetchResult>(
                OrderShipmentProvidersFetchException("error")
            )
        ).whenever(shipmentProvidersRepository)
            .fetchOrderShipmentProviders(ORDER_ID)

        setupViewModel()
        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ShowSnackbar::class.java)
        assertThat((event as ShowSnackbar).message)
            .isEqualTo(R.string.order_shipment_tracking_provider_list_error_fetch_generic)
    }

    @Test
    fun `filter results`() = testBlocking {
        doReturn(flowOf(testShipmentProvider)).whenever(shipmentProvidersRepository)
            .observeOrderShipmentProviders()

        setupViewModel()
        viewModel.onSearchQueryChanged("bbb")

        var state: ViewState? = null
        viewModel.trackingProviderListViewStateData.observeForever { _, viewState ->
            state = viewState
        }

        assertThat(state!!.providersList.size).isEqualTo(1)
        assertThat(state!!.providersList[0]).isEqualTo(testShipmentProvider[1])
    }

    @Test
    @Suppress("UNCHECKED_CAST")
    fun `handle carrier selection`() = testBlocking {
        setupViewModel()
        viewModel.onProviderSelected(testShipmentProvider[0])

        var event: Event? = null
        viewModel.event.observeForever {
            event = it
        }

        assertThat(event).isInstanceOf(ExitWithResult::class.java)
        assertThat((event as ExitWithResult<Carrier>).data.name).isEqualTo(testShipmentProvider[0].carrierName)
        assertThat((event as ExitWithResult<Carrier>).data.isCustom).isFalse()
    }
}
