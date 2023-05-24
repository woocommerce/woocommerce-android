package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.initSavedStateHandle
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Failed
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Ongoing
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.PendingDebounce
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Creation
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.ViewState
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.SelectItems
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowProductDetails
import com.woocommerce.android.ui.products.models.SiteParameters
import com.woocommerce.android.ui.products.selector.ProductSelectorViewModel
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import java.util.function.Consumer

@ExperimentalCoroutinesApi
class CreationFocusedOrderCreateEditViewModelTest : UnifiedOrderEditViewModelTest() {
    override val mode: Mode = Creation
    override val sku: String = ""
    override val tracksFlow: String = VALUE_FLOW_CREATION

    companion object {
        private const val DEFAULT_CUSTOMER_ID = 0L
    }

    override fun initMocksForAnalyticsWithOrder(order: Order) {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
    }

    @Test
    fun `when initializing the view model, then register the orderDraft flowState`() {
        verify(createUpdateOrderUseCase).invoke(any(), any())
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
    fun `when submitting customer address data, then update orderDraft liveData`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val defaultBillingAddress = Address.EMPTY.copy(firstName = "Test", lastName = "Billing")
        val defaultShippingAddress = Address.EMPTY.copy(firstName = "Test", lastName = "Shipping")

        sut.onCustomerAddressEdited(DEFAULT_CUSTOMER_ID, defaultBillingAddress, defaultShippingAddress)

        assertThat(orderDraft?.billingAddress).isEqualTo(defaultBillingAddress)
        assertThat(orderDraft?.shippingAddress).isEqualTo(defaultShippingAddress)
    }

    @Test
    fun `when submitting customer address data with empty shipping, then the billing data for both addresses`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val defaultBillingAddress = Address.EMPTY.copy(firstName = "Test", lastName = "Billing")
        val defaultShippingAddress = Address.EMPTY

        sut.onCustomerAddressEdited(DEFAULT_CUSTOMER_ID, defaultBillingAddress, defaultShippingAddress)

        assertThat(orderDraft?.billingAddress).isEqualTo(defaultBillingAddress)
        assertThat(orderDraft?.shippingAddress).isEqualTo(defaultBillingAddress)
    }

    @Test
    fun `when customer note click event is called, then trigger EditCustomerNote event`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCustomerNoteClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(EditCustomerNote::class.java)
    }

    @Test
    fun `when hitting the customer button, then trigger the EditCustomer event`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCustomerClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(EditCustomer::class.java)
    }

    @Test
    fun `when hitting the add product button, then trigger the SelectItems event`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onAddProductClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(SelectItems::class.java)
    }

    @Test
    fun `when hitting the edit order status button, then trigger ViewOrderStatusSelector event`() = testBlocking {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        val currentStatus = Order.OrderStatus("first key", "first status")

        sut.onEditOrderStatusClicked(currentStatus)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ViewOrderStatusSelector }
            ?.let { viewOrderStatusSelectorEvent ->
                assertThat(viewOrderStatusSelectorEvent.currentStatus).isEqualTo(currentStatus.statusKey)
                assertThat(viewOrderStatusSelectorEvent.orderStatusList).isEqualTo(orderStatusList.toTypedArray())
            } ?: fail("Last event should be of ViewOrderStatusSelector type")
    }

    @Test
    fun `when decreasing product quantity to zero, then call the full product view`() = testBlocking {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId
        assertThat(addedProductItem!!.quantity).isEqualTo(1F)

        sut.onDecreaseProductsQuantity(addedProductItemId)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ShowProductDetails }
            ?.let { showProductDetailsEvent ->
                assertThat(showProductDetailsEvent.item.productId).isEqualTo(123)
                assertThat(showProductDetailsEvent.item.itemId).isEqualTo(addedProductItemId)
            } ?: fail("Last event should be of ShowProductDetails type")
    }

    @Test
    fun `when decreasing variation quantity to zero, then call the full product view`() {
        val variationOrderItem = createOrderItem().copy(productId = 0, variationId = 123)
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn variationOrderItem
        }
        createSut()

        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            addedProductItem = order.items.find { it.variationId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onDecreaseProductsQuantity(addedProductItemId)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ShowProductDetails }
            ?.let { showProductDetailsEvent ->
                assertThat(showProductDetailsEvent.item.variationId).isEqualTo(123)
                assertThat(showProductDetailsEvent.item.itemId).isEqualTo(addedProductItemId)
            } ?: fail("Last event should be of ShowProductDetails type")
    }

    @Test
    fun `when decreasing product quantity to one or more, then decrease the product quantity by one`() {
        var orderDraft: Order? = null
        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            orderDraft = order
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onDecreaseProductsQuantity(addedProductItemId)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L && it.itemId == addedProductItemId }
            ?.let { assertThat(it.quantity).isEqualTo(2f) }
            ?: fail("Expected an item with productId 123 with quantity as 2")
    }

    @Test
    fun `when adding products, then update product liveData when quantity is one or more`() = testBlocking {
        var products: List<ProductUIModel> = emptyList()
        sut.products.observeForever {
            products = it
        }

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            addedProductItem = order.items.find { it.productId == 123L }
        }
        assertThat(products.size).isEqualTo(0)

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        assertThat(products.size).isEqualTo(1)
        assertThat(products.first().item.productId).isEqualTo(123)
        assertThat(products.first().item.itemId).isEqualTo(addedProductItemId)
    }

    @Test
    fun `when remove a product, then update orderDraft liveData with the quantity set to zero`() = testBlocking {
        var orderDraft: Order? = null
        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            orderDraft = order
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onRemoveProduct(addedProductItem!!)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L && it.itemId == addedProductItemId }
            ?.let { assertThat(it.quantity).isEqualTo(0f) }
            ?: fail("Expected an item with productId 123 with quantity set as 0")
    }

    @Test
    fun `when adding the very same product, then do not add a clone of the same product to the list`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.filter { it.productId == 123L }
            ?.let { addedItemsList ->
                assertThat(addedItemsList.size).isEqualTo(1)
            }
            ?: fail("Expected one product item with productId 123")
    }

    @Test
    fun `when multiple products selected, should update order's items`() = testBlocking {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(
            setOf(
                ProductSelectorViewModel.SelectedItem.Product(123),
                ProductSelectorViewModel.SelectedItem.Product(456)
            )
        )

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.let { orderItems ->
                assertThat(orderItems.size).isEqualTo(2)
                orderItems.map { it.productId }.apply {
                    assertTrue(contains(123))
                    assertTrue(contains(456))
                }
            }
            ?: fail("Expected two product items with productId 123 and 456")
    }

    @Test
    fun `when regular product and variation selected, should update order's items`() = testBlocking {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(
            setOf(
                ProductSelectorViewModel.SelectedItem.Product(123),
                ProductSelectorViewModel.SelectedItem.ProductVariation(1, 2)
            )
        )

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.let { orderItems ->
                assertThat(orderItems.size).isEqualTo(2)
                orderItems.first { it.isVariation }.let {
                    assertThat(it.productId).isEqualTo(1)
                    assertThat(it.variationId).isEqualTo(2)
                }
                orderItems.first { !it.isVariation }.let {
                    assertThat(it.productId).isEqualTo(123)
                }
            }
            ?: fail(
                "Expected one product item with productId 123 " +
                    "and one variation item with product id 1 and variation id 2"
            )
    }

    @Test
    fun `given order contains product item, when product is unselected, should be removed from order`() = testBlocking {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(emptySet())

        // then
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(0F)
        }
    }

    @Test
    fun `when removing product, should make view not editable`() = testBlocking {
        // given
        val orderItemToRemove: Order.Item = mock()

        // when
        sut.onRemoveProduct(orderItemToRemove)

        // then
        sut.viewStateData.liveData.value?.let { viewState ->
            assertThat(viewState.isEditable).isFalse
        } ?: fail("Expected view state to be not null")
    }

    @Test
    fun `when product is removed, should make view editable again`() = testBlocking {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(emptySet())

        // then
        sut.viewStateData.liveData.value?.let { viewState ->
            assertThat(viewState.isEditable).isTrue
        } ?: fail("Expected view state to be not null")
    }

    @Test
    fun `given order contains variation item, when variation is unselected, should be removed from order`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.ProductVariation(1, 2)))
        orderDraft?.items?.find { it.productId == 1L && it.variationId == 2L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(emptySet())

        // then
        orderDraft?.items?.find { it.productId == 1L && it.variationId == 2L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(0F)
        }
    }

    @Test
    fun `given order contains product item, when other product is selected, order should contain both products`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(
            setOf(
                ProductSelectorViewModel.SelectedItem.Product(123),
                ProductSelectorViewModel.SelectedItem.Product(456)
            )
        )

        // then
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }
    }

    @Test
    fun `given order contains product item, when variation is selected, order should contain original product`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(
            setOf(
                ProductSelectorViewModel.SelectedItem.Product(123),
                ProductSelectorViewModel.SelectedItem.ProductVariation(1, 2)
            )
        )

        // then
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }
    }

    @Test
    fun `given order contains product item with custom quantity, when other product is selected, the quantity should be preserved`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(1F)

            sut.onIncreaseProductsQuantity(addedProductItem.itemId)
            sut.onIncreaseProductsQuantity(addedProductItem.itemId)

            assertThat(addedProductItem.quantity).isEqualTo(1F)
        }

        // when
        sut.onProductsSelected(
            setOf(
                ProductSelectorViewModel.SelectedItem.Product(123),
                ProductSelectorViewModel.SelectedItem.Product(456)
            )
        )

        // then
        orderDraft?.items?.find { it.productId == 123L }?.let { addedProductItem ->
            assertThat(addedProductItem.quantity).isEqualTo(3F)
        }
    }

    @Test
    fun `when creating the order fails, then trigger Snackbar with fail message`() {
        orderCreateEditRepository = mock {
            onBlocking { placeOrder(defaultOrderValue) } doReturn Result.failure(Throwable())
        }
        createSut()

        val receivedEvents: MutableList<Event> = mutableListOf()
        sut.event.observeForever {
            receivedEvents.add(it)
        }

        sut.onCreateOrderClicked(defaultOrderValue)

        assertThat(receivedEvents.size).isEqualTo(1)

        receivedEvents.first()
            .run { this as? Event.ShowSnackbar }
            ?.let { showSnackbarEvent ->
                assertThat(showSnackbarEvent.message).isEqualTo(R.string.order_creation_failure_snackbar)
            } ?: fail("Event should be of ShowSnackbar type with the expected message")
    }

    @Test
    fun `when creating the order succeed, then call Order details view`() {
        val receivedEvents: MutableList<Event> = mutableListOf()
        sut.event.observeForever {
            receivedEvents.add(it)
        }

        sut.onCreateOrderClicked(defaultOrderValue)

        assertThat(receivedEvents.size).isEqualTo(2)

        receivedEvents.first()
            .run { this as? Event.ShowSnackbar }
            ?.let { showSnackbarEvent ->
                assertThat(showSnackbarEvent.message).isEqualTo(R.string.order_creation_success_snackbar)
            } ?: fail("First event should be of ShowSnackbar type with the expected message")

        receivedEvents.last()
            .run { this as? ShowCreatedOrder }
            ?.let { showCreatedOrderEvent ->
                assertThat(showCreatedOrderEvent.orderId).isEqualTo(defaultOrderValue.id)
            } ?: fail("Second event should be of ShowCreatedOrder type with the expected order ID")
    }

    @Test
    fun `when hitting the back button with changes done, then trigger discard warning dialog`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onBackButtonClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(ShowDialog::class.java)
    }

    @Test
    fun `when hitting the back button with no changes, then trigger Exit with no dialog`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onBackButtonClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(Exit::class.java)
    }

    @Test
    fun `when hitting the fee button with an existent fee, then trigger EditFee with the expected data`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        val newFeeTotal = BigDecimal(123.5)
        val orderSubtotal = (orderDraft?.total ?: BigDecimal.ZERO) - newFeeTotal
        sut.onFeeEdited(newFeeTotal)
        sut.onFeeButtonClicked()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? EditFee }
            ?.let { editFeeEvent ->
                assertThat(editFeeEvent.currentFeeValue).isEqualTo(newFeeTotal)
                assertThat(editFeeEvent.orderSubTotal).isEqualTo(orderSubtotal)
            } ?: fail("Last event should be of EditFee type")
    }

    @Test
    fun `when editing a fee, then reuse the existent one with different value`() {
        // given
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.EMPTY.copy(
                        feesLines = listOf(
                            Order.FeeLine.EMPTY.copy(id = 1, total = BigDecimal(1)),
                            Order.FeeLine.EMPTY.copy(id = 2, total = BigDecimal(2)),
                            Order.FeeLine.EMPTY.copy(id = 3, total = BigDecimal(3)),
                        )
                    )
                )
            )
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val newFeeTotal = BigDecimal(123.5)

        // when
        sut.onFeeEdited(BigDecimal(1))
        sut.onFeeEdited(BigDecimal(2))
        sut.onFeeEdited(BigDecimal(3))
        sut.onFeeEdited(newFeeTotal)

        // then
        assertThat(orderDraft?.feesLines)
            .hasSize(3)
            .first().satisfies(Consumer { firstFee -> assertThat(firstFee.total).isEqualTo(newFeeTotal) })
    }

    @Test
    fun `when removing a fee, do not remove the rest of fees`() {
        // given
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.EMPTY.copy(
                        feesLines = listOf(
                            Order.FeeLine.EMPTY.copy(id = 1, total = BigDecimal(1)),
                            Order.FeeLine.EMPTY.copy(id = 2, total = BigDecimal(2)),
                            Order.FeeLine.EMPTY.copy(id = 3, total = BigDecimal(3)),
                        )
                    )
                )
            )
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        // when
        sut.onFeeRemoved()

        // then
        assertThat(orderDraft?.feesLines)
            .hasSize(3)
            .extracting("name")
            .containsOnlyOnce(null)
            .first().matches {
                it == null
            }
    }

    @Test
    fun `when editing fees on order without fees, add one`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assert(orderDraft?.feesLines?.isEmpty() == true)

        // when
        sut.onFeeEdited(BigDecimal(1))

        // then
        assertThat(orderDraft?.feesLines).hasSize(1)
    }

    @Test
    fun `when hitting the shipping button with an existent one, then trigger EditShipping with the expected data`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        val newFeeTotal = BigDecimal(123.5)
        sut.onShippingEdited(newFeeTotal, "1")
        sut.onShippingButtonClicked()

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? EditShipping }
            ?.let { editFeeEvent ->
                val currentShippingLine = editFeeEvent.currentShippingLine
                assertThat(currentShippingLine?.total).isEqualTo(newFeeTotal)
                assertThat(currentShippingLine?.methodTitle).isEqualTo("1")
            } ?: fail("Last event should be of EditShipping type")
    }

    @Test
    fun `when editing a shipping fee, then reuse the existent one with different value`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        val newShippingFeeTotal = BigDecimal(123.5)

        sut.onShippingEdited(BigDecimal(1), "1")
        sut.onShippingEdited(BigDecimal(2), "2")
        sut.onShippingEdited(BigDecimal(3), "3")
        sut.onShippingEdited(newShippingFeeTotal, "4")

        orderDraft?.shippingLines
            ?.takeIf { it.size == 1 }
            ?.let {
                val shippingFee = it.first()
                assertThat(shippingFee.total).isEqualTo(newShippingFeeTotal)
                assertThat(shippingFee.methodTitle).isEqualTo("4")
                assertThat(shippingFee.methodId).isNotNull
            } ?: fail("Expected a shipping lines list with a single shipping fee with 123.5 as total")
    }

    @Test
    fun `when editing a shipping fee, do not remove the rest of the shipping fees`() {
        // given
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.EMPTY.copy(
                        shippingLines = listOf(
                            Order.ShippingLine("first", "first", BigDecimal(1)),
                            Order.ShippingLine("second", "second", BigDecimal(2)),
                            Order.ShippingLine("third", "third", BigDecimal(3)),
                        )
                    )
                )
            )
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        // when
        val newValue = BigDecimal(321)
        sut.onShippingEdited(newValue, "1")

        // then
        assertThat(orderDraft?.shippingLines)
            .hasSize(3)
            .first().satisfies(Consumer { firstFee -> assertThat(firstFee.total).isEqualTo(newValue) })
    }

    @Test
    fun `when order has no shipping fees, add one`() {
        // given
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assert(orderDraft?.shippingLines?.isEmpty() == true)

        // when
        sut.onShippingEdited(BigDecimal(1), "1")

        // then
        assertThat(orderDraft?.shippingLines).hasSize(1)
    }

    @Test
    fun `when removing a shipping fee, then mark the first one with null methodId`() {
        // given
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.EMPTY.copy(
                        shippingLines = listOf(
                            Order.ShippingLine("first", "first", BigDecimal(1)),
                            Order.ShippingLine("second", "second", BigDecimal(2)),
                            Order.ShippingLine("third", "third", BigDecimal(3)),
                        )
                    )
                )
            )
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        // when
        sut.onShippingRemoved()

        // then
        assertThat(orderDraft?.shippingLines?.first()?.methodId).isNull()
    }

    @Test
    fun `when OrderDraftUpdateStatus is WillStart, then adjust view state to reflect the loading preparation`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(PendingDebounce)
        }
        createSut()

        var viewState: ViewState? = null

        sut.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState).isNotNull
        assertThat(viewState?.willUpdateOrderDraft).isTrue
        assertThat(viewState?.isUpdatingOrderDraft).isFalse
        assertThat(viewState?.showOrderUpdateSnackbar).isFalse
    }

    @Test
    fun `when OrderDraftUpdateStatus is Ongoing, then adjust view state to reflect the loading`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Ongoing)
        }
        createSut()

        var viewState: ViewState? = null

        sut.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState).isNotNull
        assertThat(viewState?.willUpdateOrderDraft).isFalse
        assertThat(viewState?.isUpdatingOrderDraft).isTrue
        assertThat(viewState?.showOrderUpdateSnackbar).isFalse
    }

    @Test
    fun `when OrderDraftUpdateStatus is Succeeded, then adjust view state to reflect the loading end`() {
        val modifiedOrderValue = defaultOrderValue.copy(id = 999)
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(modifiedOrderValue))
        }
        createSut()

        var viewState: ViewState? = null
        var orderDraft: Order? = null

        sut.viewStateData.observeForever { _, new ->
            viewState = new
        }

        sut.orderDraft.observeForever {
            orderDraft = it
        }

        assertThat(viewState).isNotNull
        assertThat(viewState?.willUpdateOrderDraft).isFalse
        assertThat(viewState?.isUpdatingOrderDraft).isFalse
        assertThat(viewState?.showOrderUpdateSnackbar).isFalse

        assertThat(orderDraft).isNotNull
        assertThat(orderDraft).isEqualTo(modifiedOrderValue)
    }

    @Test
    fun `when OrderDraftUpdateStatus is Failed, then adjust view state to reflect the failure`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Failed(throwable = Throwable(message = "fail")))
        }
        createSut()

        var viewState: ViewState? = null

        sut.viewStateData.observeForever { _, new ->
            viewState = new
        }

        assertThat(viewState).isNotNull
        assertThat(viewState?.willUpdateOrderDraft).isFalse
        assertThat(viewState?.isUpdatingOrderDraft).isFalse
        assertThat(viewState?.showOrderUpdateSnackbar).isTrue
    }

    @Test
    fun `when viewState is under the order draft sync state, then canCreateOrder must be false`() {
        var viewState = ViewState(
            willUpdateOrderDraft = true,
            isUpdatingOrderDraft = false,
            showOrderUpdateSnackbar = false
        )

        assertThat(viewState.canCreateOrder).isFalse

        viewState = ViewState(
            willUpdateOrderDraft = false,
            isUpdatingOrderDraft = true,
            showOrderUpdateSnackbar = false
        )

        assertThat(viewState.canCreateOrder).isFalse

        viewState = ViewState(
            willUpdateOrderDraft = false,
            isUpdatingOrderDraft = false,
            showOrderUpdateSnackbar = true
        )

        assertThat(viewState.canCreateOrder).isFalse

        viewState = ViewState(
            willUpdateOrderDraft = false,
            isUpdatingOrderDraft = false,
            showOrderUpdateSnackbar = false
        )

        assertThat(viewState.canCreateOrder).isTrue
    }

    @Test
    fun `when hitting a product that is not synced then do nothing`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        val orderItem = Order.Item.EMPTY
        sut.onProductClicked(orderItem)

        assertThat(lastReceivedEvent).isNull()
    }

    @Test
    fun `when hitting a product that is synced then show product details`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        val orderItem = createOrderItem()
        sut.onProductClicked(orderItem)

        assertThat(lastReceivedEvent).isNotNull
        lastReceivedEvent
            .run { this as? ShowProductDetails }
            ?.let { showProductDetailsEvent ->
                val currentOrderItem = showProductDetailsEvent.item
                assertThat(currentOrderItem).isEqualTo(orderItem)
            } ?: fail("Last event should be of ShowProductDetails type")
    }

    @Test
    fun `should initialize with empty order`() {
        sut.orderDraft.observeForever {}

        assertThat(sut.orderDraft.value)
            .usingRecursiveComparison()
            .ignoringFields("dateCreated", "dateModified")
            .isEqualTo(Order.EMPTY)
    }

    @Test
    fun `when isEditable is true on the create flow the order is editable`() {
        // When the order is on Creation mode is always editable
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue.copy(isEditable = true)))
        }
        createSut()
        var lastReceivedState: ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(true)
    }

    @Test
    fun `when isEditable is false on the edit flow the order is editable`() {
        // When the order is on Creation mode is always editable
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue.copy(isEditable = false)))
        }
        createSut()
        var lastReceivedState: ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(true)
    }

    @Test
    fun `when create button tapped, send track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCreateOrderClicked(defaultOrderValue)

        verify(tracker).track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_STATUS to defaultOrderValue.status,
                AnalyticsTracker.KEY_PRODUCT_COUNT to sut.products.value?.count(),
                AnalyticsTracker.KEY_HAS_CUSTOMER_DETAILS to defaultOrderValue.billingAddress.hasInfo(),
                AnalyticsTracker.KEY_HAS_FEES to defaultOrderValue.feesLines.isNotEmpty(),
                AnalyticsTracker.KEY_HAS_SHIPPING_METHOD to defaultOrderValue.shippingLines.isNotEmpty()
            )
        )
    }

    @Test
    fun `when coupon added should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponEntered("code")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_CREATION)
        )
    }

    @Test
    fun `when coupon removed should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponEntered("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_CREATION)
        )
    }

    @Test
    fun `given sku, when view model init, then fetch product information`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                OrderCreateEditViewModel.Mode.Creation, "123"
            ).initSavedStateHandle()
            whenever(parameterRepository.getParameters("parameters_key", navArgs)).thenReturn(
                SiteParameters(
                    currencyCode = "",
                    currencySymbol = null,
                    currencyFormattingParameters = null,
                    weightUnit = null,
                    dimensionUnit = null,
                    gmtOffset = 0F
                )
            )

            createSut(navArgs)

            verify(productListRepository).searchProductList(
                "123",
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }

    @Test
    fun `given empty sku, when view model init, then do not fetch product information`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                OrderCreateEditViewModel.Mode.Creation, ""
            ).initSavedStateHandle()
            whenever(parameterRepository.getParameters("parameters_key", navArgs)).thenReturn(
                SiteParameters(
                    currencyCode = "",
                    currencySymbol = null,
                    currencyFormattingParameters = null,
                    weightUnit = null,
                    dimensionUnit = null,
                    gmtOffset = 0F
                )
            )

            createSut(navArgs)

            verify(productListRepository, never()).searchProductList(
                "123",
                WCProductStore.SkuSearchOptions.ExactSearch
            )
        }
    }
}
