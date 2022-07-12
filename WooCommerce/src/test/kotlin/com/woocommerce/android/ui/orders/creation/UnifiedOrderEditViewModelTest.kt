package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import java.math.BigDecimal

@ExperimentalCoroutinesApi
abstract class UnifiedOrderEditViewModelTest : BaseUnitTest() {
    protected lateinit var sut: OrderCreationViewModel
    protected lateinit var viewState: OrderCreationViewModel.ViewState
    protected lateinit var savedState: SavedStateHandle
    protected lateinit var mapItemToProductUIModel: MapItemToProductUiModel
    protected lateinit var createUpdateOrderUseCase: CreateUpdateOrder
    protected lateinit var autoSyncPriceModifier: AutoSyncPriceModifier
    protected lateinit var autoSyncOrder: AutoSyncOrder
    protected lateinit var createOrderItemUseCase: CreateOrderItem
    protected lateinit var orderCreationRepository: OrderCreationRepository
    protected lateinit var orderDetailRepository: OrderDetailRepository
    protected lateinit var parameterRepository: ParameterRepository
    private lateinit var determineMultipleLinesContext: DetermineMultipleLinesContext
    protected lateinit var tracker: AnalyticsTrackerWrapper

    protected val defaultOrderValue = Order.EMPTY.copy(id = 123)

    @Before
    fun setUp() {
        initMocks()
        createSut()
    }

    protected abstract val mode: OrderCreationViewModel.Mode

    private fun initMocks() {
        val defaultOrderItem = createOrderItem()
        val emptyOrder = Order.EMPTY
        viewState = OrderCreationViewModel.ViewState()
        savedState = spy(OrderCreationFormFragmentArgs(mode).toSavedStateHandle()) {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on { getLiveData(eq(Order.EMPTY.javaClass.name), any<Order>()) } doReturn MutableLiveData(emptyOrder)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(Order.EMPTY))
        }
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn defaultOrderItem
        }
        parameterRepository = mock {
            on { getParameters("parameters_key", savedState) } doReturn
                SiteParameters(
                    currencyCode = "",
                    currencySymbol = null,
                    currencyFormattingParameters = null,
                    weightUnit = null,
                    dimensionUnit = null,
                    gmtOffset = 0F
                )
        }
        orderCreationRepository = mock {
            onBlocking { placeOrder(defaultOrderValue) } doReturn Result.success(defaultOrderValue)
        }
        orderDetailRepository = mock {
            on { getOrderStatusOptions() } doReturn orderStatusList
        }
        mapItemToProductUIModel = mock {
            onBlocking { invoke(any()) } doReturn ProductUIModel(
                item = defaultOrderItem,
                imageUrl = "",
                isStockManaged = false,
                stockQuantity = 0.0,
                stockStatus = ProductStockStatus.InStock
            )
        }
        determineMultipleLinesContext = mock {
            on { invoke(any()) } doReturn OrderCreationViewModel.MultipleLinesContext.None
        }
        tracker = mock()
    }

    protected abstract val tracksFlow: String

    @Test
    fun `when product selected, send tracks event`() {
        sut.onProductSelected(123)

        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
        )
    }

    @Test
    fun `when customer address edited, send tracks event`() {
        sut.onCustomerAddressEdited(Address.EMPTY, Address.EMPTY)

        verify(tracker).track(
            AnalyticsEvent.ORDER_CUSTOMER_ADD,
            mapOf(
                AnalyticsTracker.KEY_FLOW to tracksFlow,
                AnalyticsTracker.KEY_HAS_DIFFERENT_SHIPPING_DETAILS to false,
            )
        )
    }

    @Test
    fun `when fee edited, send tracks event`() {
        sut.onFeeEdited(BigDecimal.TEN)

        verify(tracker).track(
            AnalyticsEvent.ORDER_FEE_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
        )
    }

    @Test
    fun `when shipping added or edited, send tracks event`() {
        sut.onShippingEdited(BigDecimal.TEN, "")

        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
        )
    }

    @Test
    fun `when customer note added or edited, send tracks event`() {
        sut.onCustomerNoteEdited("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_NOTE_ADD,
            mapOf(
                AnalyticsTracker.KEY_PARENT_ID to 0L,
                AnalyticsTracker.KEY_STATUS to Order.Status.Pending,
                AnalyticsTracker.KEY_TYPE to AnalyticsTracker.Companion.OrderNoteType.CUSTOMER,
                AnalyticsTracker.KEY_FLOW to tracksFlow,
            )
        )
    }

    @Test
    fun `when status is edited, send tracks event`() {
        sut.onOrderStatusChanged(Order.Status.Cancelled)

        verify(tracker).track(
            AnalyticsEvent.ORDER_STATUS_CHANGE,
            mapOf(
                AnalyticsTracker.KEY_ID to 0L,
                AnalyticsTracker.KEY_FROM to Order.Status.Pending.value,
                AnalyticsTracker.KEY_TO to Order.Status.Cancelled.value,
                AnalyticsTracker.KEY_FLOW to tracksFlow
            )
        )
    }

    protected fun createSut() {
        autoSyncPriceModifier = AutoSyncPriceModifier(createUpdateOrderUseCase)
        autoSyncOrder = AutoSyncOrder(createUpdateOrderUseCase)
        sut = OrderCreationViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = orderDetailRepository,
            orderCreationRepository = orderCreationRepository,
            mapItemToProductUiModel = mapItemToProductUIModel,
            createOrderItem = createOrderItemUseCase,
            determineMultipleLinesContext = determineMultipleLinesContext,
            parameterRepository = parameterRepository,
            autoSyncOrder = autoSyncOrder,
            autoSyncPriceModifier = autoSyncPriceModifier,
            tracker = tracker
        )
    }

    protected fun createOrderItem(withId: Long = 123) =
        Order.Item.EMPTY.copy(
            productId = withId,
            itemId = (1L..1000000000L).random()
        )

    protected val orderStatusList = listOf(
        Order.OrderStatus("first key", "first status"),
        Order.OrderStatus("second key", "second status"),
        Order.OrderStatus("third key", "third status")
    )
}
