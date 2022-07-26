package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Failed
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
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import java.math.BigDecimal

@ExperimentalCoroutinesApi
abstract class UnifiedOrderEditViewModelTest : BaseUnitTest() {
    protected lateinit var sut: OrderCreateEditViewModel
    protected lateinit var viewState: OrderCreateEditViewModel.ViewState
    protected lateinit var savedState: SavedStateHandle
    protected lateinit var mapItemToProductUIModel: MapItemToProductUiModel
    protected lateinit var createUpdateOrderUseCase: CreateUpdateOrder
    protected lateinit var autoSyncPriceModifier: AutoSyncPriceModifier
    protected lateinit var autoSyncOrder: AutoSyncOrder
    protected lateinit var createOrderItemUseCase: CreateOrderItem
    protected lateinit var orderCreateEditRepository: OrderCreateEditRepository
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

    protected abstract val mode: OrderCreateEditViewModel.Mode

    private fun initMocks() {
        val defaultOrderItem = createOrderItem()
        val emptyOrder = Order.EMPTY
        viewState = OrderCreateEditViewModel.ViewState()
        savedState = spy(OrderCreateEditFormFragmentArgs(mode).toSavedStateHandle()) {
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
        orderCreateEditRepository = mock {
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
            on { invoke(any()) } doReturn OrderCreateEditViewModel.MultipleLinesContext.None
        }
        tracker = mock()
    }

    protected abstract val tracksFlow: String

    protected abstract fun initMocksForAnalyticsWithOrder(order: Order)

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

    @Test
    open fun `when product quantity increased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onIncreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product quantity decreased, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product quantity decreased but quantity 1, don't send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 1F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onDecreaseProductsQuantity(productId)
        verify(tracker, never()).track(
            AnalyticsEvent.ORDER_PRODUCT_QUANTITY_CHANGE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when product removed, send tracks event`() {
        val productId = 1L
        val products = OrderTestUtils.generateTestOrderItems(count = 1, productId = productId, quantity = 3F)
        val order = defaultOrderValue.copy(items = products)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onRemoveProduct(products.first())
        verify(tracker).track(
            AnalyticsEvent.ORDER_PRODUCT_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when fee removed, send tracks event`() {
        val feesLines = listOf(
            Order.FeeLine.EMPTY.copy(
                name = "order_custom_fee",
                total = BigDecimal(10)
            )
        )
        val order = defaultOrderValue.copy(feesLines = feesLines)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onFeeRemoved()
        verify(tracker).track(
            AnalyticsEvent.ORDER_FEE_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when shipping method removed, send tracks event`() {
        val shippingLines = listOf(
            Order.ShippingLine(
                methodId = "other",
                total = BigDecimal(10),
                methodTitle = "name"
            )
        )
        val order = defaultOrderValue.copy(shippingLines = shippingLines)
        initMocksForAnalyticsWithOrder(order)
        createSut()
        sut.onShippingRemoved()
        verify(tracker).track(
            AnalyticsEvent.ORDER_SHIPPING_METHOD_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow)
        )
    }

    @Test
    fun `when order sync fails, send tracks event`() {
        val wooError = WooError(
            type = WooErrorType.GENERIC_ERROR,
            original = BaseRequest.GenericErrorType.TIMEOUT,
            message = "fail"
        )
        val throwable = WooException(error = wooError)
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Failed(throwable))
        }

        createSut()

        verify(tracker).track(
            stat = AnalyticsEvent.ORDER_SYNC_FAILED,
            properties = mapOf(AnalyticsTracker.KEY_FLOW to tracksFlow),
            errorContext = sut::class.java.simpleName,
            errorType = wooError.type.name,
            errorDescription = wooError.message
        )
    }

    protected fun createSut() {
        autoSyncPriceModifier = AutoSyncPriceModifier(createUpdateOrderUseCase)
        autoSyncOrder = AutoSyncOrder(createUpdateOrderUseCase)
        sut = OrderCreateEditViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = orderDetailRepository,
            orderCreateEditRepository = orderCreateEditRepository,
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
