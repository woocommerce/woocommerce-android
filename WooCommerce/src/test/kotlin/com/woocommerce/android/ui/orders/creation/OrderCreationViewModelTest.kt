package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.products.ParameterRepository
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.MultiLiveEvent
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus

@ExperimentalCoroutinesApi
class OrderCreationViewModelTest: BaseUnitTest() {
    private lateinit var sut: OrderCreationViewModel
    private lateinit var viewState: ViewState
    private lateinit var savedState: SavedStateHandle
    private lateinit var mapItemToProductUIModel: MapItemToProductUiModel
    private lateinit var createOrUpdateOrderUseCase: CreateOrUpdateOrderDraft
    private lateinit var createOrderItemUseCase: CreateOrderItem
    private lateinit var parameterRepository: ParameterRepository

    @Before
    fun setUp() {
        initMocks()
        createSut()
    }

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {
        verify(createOrUpdateOrderUseCase).invoke(any(), any())
    }

    @Test
    fun `when submitting customer note, then update orderDraft liveData`() {
        var orderDraft: Order? = null

        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onCustomerNoteEdited("Customer note test")

        assertThat(orderDraft?.customerNote).isEqualTo("Customer note test")
    }

    @Test
    fun `when customer note click event is called, then trigger EditCustomerNote event`() {
        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCustomerNoteClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(EditCustomerNote::class.java)
    }

    @Test
    fun `when submitting order status, then update orderDraft liveData`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        Order.Status.fromDataModel(CoreOrderStatus.COMPLETED)
            ?.let { sut.onOrderStatusChanged(it) }
            ?: fail("Failed to submit an order status")

        assertThat(orderDraft?.status).isEqualTo(Order.Status.fromDataModel(CoreOrderStatus.COMPLETED))

        Order.Status.fromDataModel(CoreOrderStatus.ON_HOLD)
            ?.let { sut.onOrderStatusChanged(it) }
            ?: fail("Failed to submit an order status")

        assertThat(orderDraft?.status).isEqualTo(Order.Status.fromDataModel(CoreOrderStatus.ON_HOLD))
    }

    @Test
    fun `when decreasing product quantity to zero, then call the full product view`() = runBlockingTest {
        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onDecreaseProductsQuantity(123)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ShowProductDetails }
            ?.let { showProductDetailsEvent ->
                assertThat(showProductDetailsEvent.item.productId).isEqualTo(123)
            } ?: fail("Last event should be of ShowProductDetails type")
    }

    @Test
    fun `when decreasing variation quantity to zero, then call the full product view`() {
        val variationOrderItem = createOrderItem().copy(productId = 0, variationId = 123)
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn variationOrderItem
        }
        createSut()

        var lastReceivedEvent: MultiLiveEvent.Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onDecreaseProductsQuantity(123)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ShowProductDetails }
            ?.let { showProductDetailsEvent ->
                assertThat(showProductDetailsEvent.item.variationId).isEqualTo(123)
            } ?: fail("Last event should be of ShowProductDetails type")
    }

    @Test
    fun `when decreasing product quantity to one or more, then decrease the product quantity by one`() = runBlockingTest {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onDecreaseProductsQuantity(123)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L }
            ?.let { assertThat(it.quantity).isEqualTo(1f) }
            ?: fail("Expected an item with productId 123 with quantity as 1")
    }

    @Test
    fun `when adding products, then update product liveData when quantity is one or more`() = runBlockingTest {
        var products: List<ProductUIModel> = emptyList()
        sut.products.observeForever {
            products = it
        }

        sut.onProductSelected(123)
        assertThat(products).isEmpty()

        sut.onIncreaseProductsQuantity(123)
        assertThat(products.size).isEqualTo(1)
        assertThat(products.first().item.productId).isEqualTo(123)
    }

    @Test
    fun `when remove a product, then update orderDraft liveData with the quantity set to zero`() = runBlockingTest {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        val addedItem = orderDraft?.items?.first() ?: fail("Added item should exist")
        sut.onRemoveProduct(addedItem)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L }
            ?.let { assertThat(it.quantity).isEqualTo(0f) }
            ?: fail("Expected an item with productId 123 with quantity set as 0")
    }

    @Test
    fun `when adding the very same product, then increase item quantity by one`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onProductSelected(123)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L }
            ?.let { assertThat(it.quantity).isEqualTo(2f) }
            ?: fail("Expected an item with productId 123 with quantity as 2")
    }

    @Test
    fun `when adding customer address with empty shipping, then set shipping as billing`() {

    }

    @Test
    fun `when creating the order fails, then trigger Snackbar with fail message`() {

    }

    @Test
    fun `when creating the order succeed, then call Order details view`() {

    }

    @Test
    fun `when hitting the back button with changes done, then trigger discard warning dialog`() {

    }

    @Test
    fun `when editing a fee, then reuse the existent one with different value`() {

    }

    @Test
    fun `when editing a shipping fee, then reuse the existent one with different value`() {

    }

    private fun createSut() {
        sut = OrderCreationViewModel(
            savedState = savedState,
            dispatchers = coroutinesTestRule.testDispatchers,
            orderDetailRepository = mock(),
            orderCreationRepository = mock(),
            mapItemToProductUiModel = mapItemToProductUIModel,
            createOrUpdateOrderDraft = createOrUpdateOrderUseCase,
            createOrderItem = createOrderItemUseCase,
            parameterRepository = parameterRepository
        )
    }

    private fun initMocks() {
        val defaultOrderItem = createOrderItem()
        viewState = ViewState()
        savedState = mock {
            on { getLiveData(viewState.javaClass.name, viewState) } doReturn MutableLiveData(viewState)
            on { getLiveData(Order.EMPTY.javaClass.name, Order.EMPTY) } doReturn MutableLiveData(Order.EMPTY)
        }
        createOrUpdateOrderUseCase = mock()
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
        mapItemToProductUIModel = mock {
            onBlocking { invoke(any()) } doReturn ProductUIModel(
                item = defaultOrderItem,
                imageUrl = "",
                isStockManaged = false,
                stockQuantity = 0.0
            )
        }
    }

    private fun createOrderItem(withId: Long = 123) = Order.Item.EMPTY.copy(productId = withId)
}
