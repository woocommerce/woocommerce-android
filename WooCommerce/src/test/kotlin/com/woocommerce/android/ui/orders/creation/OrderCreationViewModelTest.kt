package com.woocommerce.android.ui.orders.creation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.SavedStateHandle
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.ViewState
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

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {
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
    fun `when decreasing product quantity to one or more, then decrease the product quantity by one`() = runBlockingTest {
        var lastReceivedEvent: MultiLiveEvent.Event? = null
        var orderDraft: Order? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductSelected(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onIncreaseProductsQuantity(123)
        sut.onDecreaseProductsQuantity(123)

        assertThat(lastReceivedEvent).isNull()
        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L }
            ?.let { assertThat(it.quantity).isEqualTo(1f) }
            ?: fail("Expected an item with productId 123 with quantity as 1")
    }

    @Test
    fun `when adding products, then update product liveData when quantity is one or more`() = runBlockingTest {
        var lastReceivedEvent: MultiLiveEvent.Event? = null
        var products: List<ProductUIModel> = emptyList()
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.products.observeForever {
            products = it
        }

        sut.onProductSelected(123)
        assertThat(products).isEmpty()

        sut.onIncreaseProductsQuantity(123)
        assertThat(lastReceivedEvent).isNull()
        assertThat(products.size).isEqualTo(1)
        assertThat(products.first().item.productId).isEqualTo(123)
    }

    @Test
    fun `when adding the very same product, then increase item quantity by one`() {

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

    private fun createOrderItem(withId: Long = 123) = Order.Item.EMPTY.copy(productId = withId)
}
