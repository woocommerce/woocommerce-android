package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.details.OrderDetailRepository
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.ProductStockStatus
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy

@ExperimentalCoroutinesApi
abstract class UnifiedOrderEditViewModelTest : BaseUnitTest() {
    protected lateinit var sut: OrderCreationViewModel
    protected lateinit var viewState: OrderCreationViewModel.ViewState
    protected lateinit var savedState: SavedStateHandle
    protected lateinit var mapItemToProductUIModel: MapItemToProductUiModel
    protected lateinit var createOrUpdateOrderUseCase: CreateOrUpdateOrderDraft
    protected lateinit var createOrderItemUseCase: CreateOrderItem
    protected lateinit var orderCreationRepository: OrderCreationRepository
    protected lateinit var orderDetailRepository: OrderDetailRepository
    protected lateinit var parameterRepository: ParameterRepository

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
        createOrUpdateOrderUseCase = mock {
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
    }

    protected fun createSut() {
        sut = OrderCreationViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = orderDetailRepository,
            orderCreationRepository = orderCreationRepository,
            mapItemToProductUiModel = mapItemToProductUIModel,
            createOrUpdateOrderDraft = createOrUpdateOrderUseCase,
            createOrderItem = createOrderItemUseCase,
            parameterRepository = parameterRepository
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
