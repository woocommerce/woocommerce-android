package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.R
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.Failed
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.Ongoing
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.PendingDebounce
import com.woocommerce.android.ui.orders.creation.CreateOrUpdateOrderDraft.OrderDraftUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.Mode.Creation
import com.woocommerce.android.ui.orders.creation.OrderCreationViewModel.ViewState
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.AddProduct
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.EditShipping
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreationNavigationTarget.ShowProductDetails
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowDialog
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import java.math.BigDecimal

@ExperimentalCoroutinesApi
class CreationFocusedOrderCreationViewModelTest : UnifiedOrderEditViewModelTest() {
    override val mode: Mode = Creation

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

        sut.onCustomerAddressEdited(defaultBillingAddress, defaultShippingAddress)

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

        sut.onCustomerAddressEdited(defaultBillingAddress, defaultShippingAddress)

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
    fun `when hitting the add product button, then trigger the AddProduct event`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onAddProductClicked()

        assertThat(lastReceivedEvent).isNotNull
        assertThat(lastReceivedEvent).isInstanceOf(AddProduct::class.java)
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

        sut.onProductSelected(123)

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
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

        sut.onProductSelected(123)

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
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

        sut.onProductSelected(123)

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onIncreaseProductsQuantity(addedProductItemId)
        sut.onDecreaseProductsQuantity(addedProductItemId)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L && it.itemId == addedProductItemId }
            ?.let { assertThat(it.quantity).isEqualTo(1f) }
            ?: fail("Expected an item with productId 123 with quantity as 1")
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

        sut.onProductSelected(123)

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        assertThat(products).isEmpty()

        sut.onIncreaseProductsQuantity(addedProductItemId)

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

        sut.onProductSelected(123)

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
    fun `when adding the very same product, then add a clone of the same product to the list`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        sut.onProductSelected(123)
        sut.onProductSelected(123)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.filter { it.productId == 123L }
            ?.let { addedItemsList ->
                assertThat(addedItemsList.size).isEqualTo(2)
            }
            ?: fail("Expected two product items with productId 123")
    }

    @Test
    fun `when creating the order fails, then trigger Snackbar with fail message`() {
        orderCreationRepository = mock {
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

        sut.onProductSelected(123)

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
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        val newFeeTotal = BigDecimal(123.5)

        sut.onFeeEdited(BigDecimal(1))
        sut.onFeeEdited(BigDecimal(2))
        sut.onFeeEdited(BigDecimal(3))
        sut.onFeeEdited(newFeeTotal)

        orderDraft?.feesLines
            ?.takeIf { it.size == 1 }
            ?.let {
                val currentFee = it.first()
                assertThat(currentFee.total).isEqualTo(newFeeTotal)
                assertThat(currentFee.name).isNotNull
            } ?: fail("Expected a fee lines list with a single fee with 123.5 as total")
    }

    @Test
    fun `when removing a fee, then mark the existent one with null name`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        val newFeeTotal = BigDecimal(123.5)
        sut.onFeeEdited(newFeeTotal)
        sut.onFeeRemoved()

        orderDraft?.feesLines
            ?.takeIf { it.size == 1 }
            ?.let {
                val currentFee = it.first()
                assertThat(currentFee.total).isEqualTo(newFeeTotal)
                assertThat(currentFee.name).isNull()
            } ?: fail("Expected a fee lines list with a single fee with 123.5 as total")
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
    fun `when removing a shipping fee, then mark the existent one with null methodId`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        val newShippingFeeTotal = BigDecimal(123.5)
        sut.onShippingEdited(newShippingFeeTotal, "4")
        sut.onShippingRemoved()

        orderDraft?.shippingLines
            ?.takeIf { it.size == 1 }
            ?.let {
                val shippingFee = it.first()
                assertThat(shippingFee.total).isEqualTo(newShippingFeeTotal)
                assertThat(shippingFee.methodTitle).isEqualTo("4")
                assertThat(shippingFee.methodId).isNull()
            } ?: fail("Expected a shipping lines list with a single shipping fee with 123.5 as total")
    }

    @Test
    fun `when OrderDraftUpdateStatus is WillStart, then adjust view state to reflect the loading preparation`() {
        createOrUpdateOrderUseCase = mock {
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
        createOrUpdateOrderUseCase = mock {
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
        createOrUpdateOrderUseCase = mock {
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
        createOrUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Failed)
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

        assertThat(sut.orderDraft.value).isEqualToIgnoringGivenFields(Order.EMPTY, "dateCreated", "dateModified")
    }

    @Test
    fun `when isEditable is true on the create flow the order is editable`() {
        // When the order is on Creation mode is always editable
        createOrUpdateOrderUseCase = mock {
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
        createOrUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue.copy(isEditable = false)))
        }
        createSut()
        var lastReceivedState: ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(true)
    }
}
