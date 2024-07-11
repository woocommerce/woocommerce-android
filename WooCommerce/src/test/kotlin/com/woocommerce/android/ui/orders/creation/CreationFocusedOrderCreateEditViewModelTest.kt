package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.R
import com.woocommerce.android.WooException
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsEvent.ADD_CUSTOM_AMOUNT_DONE_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ADD_CUSTOM_AMOUNT_NAME_ADDED
import com.woocommerce.android.analytics.AnalyticsEvent.ADD_CUSTOM_AMOUNT_PERCENTAGE_ADDED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_CREATION_REMOVE_CUSTOM_AMOUNT_TAPPED
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_ADD
import com.woocommerce.android.analytics.AnalyticsEvent.ORDER_FEE_UPDATE
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_CUSTOM_AMOUNT_TAX_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_EXPANDED
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_FLOW
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_BUNDLE_CONFIGURATION
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_CUSTOMER_DETAILS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_FEES
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HAS_SHIPPING_METHOD
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_HORIZONTAL_SIZE_CLASS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_ADDED_VIA
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_PRODUCT_COUNT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_BARCODE_FORMAT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_SCANNING_FAILURE_REASON
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.KEY_STATUS
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CUSTOM_AMOUNT_TAX_STATUS_NONE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_CUSTOM_AMOUNT_TAX_STATUS_TAXABLE
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_CREATION
import com.woocommerce.android.model.Address
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.CustomAmountUIModel
import com.woocommerce.android.ui.orders.OrderNavigationTarget.ViewOrderStatusSelector
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Failed
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Ongoing
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.PendingDebounce
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Creation
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.ViewState
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponDetailsViewModel
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomer
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditCustomerNote
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.EditFee
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.SelectItems
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget.ShowCreatedOrder
import com.woocommerce.android.ui.orders.creation.shipping.ShippingUpdateResult
import com.woocommerce.android.ui.orders.creation.taxes.TaxBasedOnSetting
import com.woocommerce.android.ui.orders.creation.totals.TotalsSectionsState
import com.woocommerce.android.ui.orders.creation.views.ProductAmountEvent
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsFragment.Companion.CUSTOM_AMOUNT
import com.woocommerce.android.ui.payments.customamounts.CustomAmountsViewModel
import com.woocommerce.android.ui.products.ProductTestUtils
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
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.BaseRequest
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooError
import org.wordpress.android.fluxc.network.rest.wpcom.wc.WooErrorType
import org.wordpress.android.fluxc.network.rest.wpcom.wc.order.CoreOrderStatus
import org.wordpress.android.fluxc.store.WCProductStore
import java.math.BigDecimal
import java.util.Date
import java.util.function.Consumer
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class CreationFocusedOrderCreateEditViewModelTest : UnifiedOrderEditViewModelTest() {
    override val mode: Mode = Creation()
    override val sku: String = ""
    override val barcodeFormat: BarcodeFormat = BarcodeFormat.FormatUPCA
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
    fun `when initializing the view model, then fetch current tax setting`() = testBlocking {
        verify(orderCreateEditRepository).fetchTaxBasedOnSetting()
    }

    @Test
    fun `given tax based on store when initializing the view model, then update view state with current tax setting`() =
        testBlocking {
            whenever(orderCreateEditRepository.fetchTaxBasedOnSetting()).thenReturn(
                TaxBasedOnSetting.StoreAddress
            )
            whenever(resourceProvider.getString(R.string.order_creation_tax_based_on_store_address))
                .thenReturn("Calculated on store address")
            createSut()
            val viewState = sut.viewStateData.liveData.value!!
            assertThat(viewState.taxBasedOnSettingLabel).isEqualTo("Calculated on store address")
        }

    @Test
    fun `given tax based on shipping when initializing the view model, then update view state with current tax setting`() =
        testBlocking {
            whenever(orderCreateEditRepository.fetchTaxBasedOnSetting()).thenReturn(
                TaxBasedOnSetting.ShippingAddress
            )
            whenever(resourceProvider.getString(R.string.order_creation_tax_based_on_shipping_address))
                .thenReturn("Calculated on shipping address")
            whenever(resourceProvider.getString(R.string.order_creation_set_tax_rate))
                .thenReturn("Set New Tax Rate")
            createSut()
            val viewState = sut.viewStateData.liveData.value!!
            assertThat(viewState.taxBasedOnSettingLabel).isEqualTo("Calculated on shipping address")
        }

    @Test
    fun `given tax based on billing when initializing the view model, then update view state with current tax setting`() =
        testBlocking {
            whenever(orderCreateEditRepository.fetchTaxBasedOnSetting()).thenReturn(TaxBasedOnSetting.BillingAddress)
            whenever(resourceProvider.getString(R.string.order_creation_tax_based_on_billing_address))
                .thenReturn("Calculated on billing address")
            whenever(resourceProvider.getString(R.string.order_creation_set_tax_rate))
                .thenReturn("Set New Tax Rate")
            createSut()
            val viewState = sut.viewStateData.liveData.value!!
            assertThat(viewState.taxBasedOnSettingLabel).isEqualTo("Calculated on billing address")
        }

    @Test
    fun `when tax help clicked, then should track event`() = testBlocking {
        whenever(orderCreateEditRepository.getTaxBasedOnSetting()).thenReturn(TaxBasedOnSetting.BillingAddress)
        val mockedSite = SiteModel().also { it.adminUrl = "https://test.com" }
        whenever(selectedSite.get()).thenReturn(mockedSite)
        whenever(resourceProvider.getString(anyInt(), anyString()))
            .thenReturn("Your tax rate is currently calculated based on your billing address:")
        sut.onTaxHelpButtonClicked()
        verify(tracker).track(AnalyticsEvent.ORDER_TAXES_HELP_BUTTON_TAPPED)
    }

    @Test
    fun `when set new tax rate clicked, then should track event`() = testBlocking {
        val mockedSite = SiteModel().also { it.adminUrl = "https://test.com" }
        whenever(selectedSite.get()).thenReturn(mockedSite)
        sut.onSetTaxRateClicked()
        verify(
            tracker
        ).track(AnalyticsEvent.ORDER_CREATION_SET_NEW_TAX_RATE_TAPPED, mapOf(KEY_HORIZONTAL_SIZE_CLASS to "compact"))
    }

    @Test
    fun `when onSetNewTaxRateClicked, then should track event`() = testBlocking {
        val mockedSite = SiteModel().also { it.adminUrl = "https://test.com" }
        whenever(selectedSite.get()).thenReturn(mockedSite)
        sut.onSetNewTaxRateClicked()
        verify(
            tracker
        ).track(
            AnalyticsEvent.TAX_RATE_AUTO_TAX_RATE_SET_NEW_RATE_FOR_ORDER_TAPPED,
            mapOf(KEY_HORIZONTAL_SIZE_CLASS to "compact")
        )
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

        sut.onCustomerEdited(
            Order.Customer(
                DEFAULT_CUSTOMER_ID,
                billingAddress = defaultBillingAddress,
                shippingAddress = defaultShippingAddress
            )
        )

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

        sut.onCustomerEdited(
            Order.Customer(
                DEFAULT_CUSTOMER_ID,
                billingAddress = defaultBillingAddress,
                shippingAddress = defaultShippingAddress
            )
        )

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

        sut.onEditCustomerClicked()

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
    fun `when decreasing product quantity to zero, then remove product from order`() = testBlocking {
        var orderDraft: Order? = null
        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            orderDraft = order
            addedProductItem = order.items.find { it.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onDecreaseProductsQuantity(addedProductItemId)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.productId == 123L && it.itemId == addedProductItemId }
            ?.let { assertThat(it.quantity).isEqualTo(0f) }
            ?: fail("Expected an item with productId 123 with quantity set as 0")
    }

    @Test
    fun `when decreasing variation quantity to zero, then remove product from order`() {
        var orderDraft: Order? = null
        val variationOrderItem = createOrderItem().copy(productId = 0, variationId = 123)
        createOrderItemUseCase = mock {
            onBlocking { invoke(123, null) } doReturn variationOrderItem
        }

        createSut()

        var addedProductItem: Order.Item? = null
        sut.orderDraft.observeForever { order ->
            orderDraft = order
            addedProductItem = order.items.find { it.variationId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProductItem).isNotNull
        val addedProductItemId = addedProductItem!!.itemId

        sut.onDecreaseProductsQuantity(addedProductItemId)

        orderDraft?.items
            ?.takeIf { it.isNotEmpty() }
            ?.find { it.variationId == 123L && it.itemId == addedProductItemId }
            ?.let { assertThat(it.quantity).isEqualTo(0f) }
            ?: fail("Expected an item with productId 123 with quantity set as 0")
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
        var products: List<OrderCreationProduct> = emptyList()
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
        var addedProduct: OrderCreationProduct? = null
        sut.orderDraft.observeForever { order ->
            orderDraft = order
        }

        sut.products.observeForever { productList ->
            addedProduct = productList.find { it.item.productId == 123L }
        }

        sut.onProductsSelected(setOf(ProductSelectorViewModel.SelectedItem.Product(123)))

        assertThat(addedProduct).isNotNull

        val addedProductItemId = addedProduct!!.item.itemId

        sut.onItemAmountChanged(addedProduct!!, ProductAmountEvent.Increase)
        sut.onRemoveProduct(addedProduct!!)

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

        var products: List<OrderCreationProduct>? = null
        sut.products.observeForever {
            products = it
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

        assertThat(products?.size).isEqualTo(0)
    }

    @Test
    fun `when removing product, should make view not editable`() = testBlocking {
        // given
        val orderProductToRemove: OrderCreationProduct = mock()

        // when
        sut.onRemoveProduct(orderProductToRemove)

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

        var products: List<OrderCreationProduct>? = null
        sut.products.observeForever {
            products = it
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

        assertThat(products?.size).isEqualTo(0)
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
            onBlocking { createOrUpdateOrder(defaultOrderValue) } doReturn Result.failure(Throwable())
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
                    Order.getEmptyOrder(Date(), Date()).copy(
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
                    Order.getEmptyOrder(Date(), Date()).copy(
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
    fun `when editing a shipping fee, then reuse the existent one with different value`() {
        val itemId = 2L
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        shippingLines = listOf(
                            Order.ShippingLine(itemId, "first", "first", BigDecimal(1), BigDecimal.ZERO),
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

        val newValue = BigDecimal(125.3)
        val result = ShippingUpdateResult(
            id = itemId,
            amount = newValue,
            methodId = "first",
            name = "first"
        )
        sut.onUpdatedShipping(result)

        assertThat(orderDraft?.shippingLines).hasSize(3)
        val line = orderDraft?.shippingLines?.find { it.itemId == itemId }
        assertNotNull(line)
        assertThat(line.total).isEqualTo(newValue)
        assertThat(line.methodTitle).isEqualTo("first")
        assertThat(line.methodId).isNotNull
    }

    @Test
    fun `when editing a shipping fee, do not remove the rest of the shipping fees`() {
        // given
        val itemId = 2L
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        shippingLines = listOf(
                            Order.ShippingLine(itemId, "first", "first", BigDecimal(1), BigDecimal.ZERO),
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
        val result = ShippingUpdateResult(
            id = itemId,
            amount = newValue,
            methodId = "first",
            name = "first"
        )
        sut.onUpdatedShipping(result)

        // then
        assertThat(orderDraft?.shippingLines).hasSize(3)
        val item = orderDraft?.shippingLines?.find { it.itemId == itemId }
        assertNotNull(item)
        assertThat(item.total).isEqualTo(newValue)
    }

    @Test
    fun `when order has no shipping fees, add one`() {
        // given
        val result = ShippingUpdateResult(
            id = null,
            amount = BigDecimal.TEN,
            methodId = "first",
            name = "first"
        )
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assert(orderDraft?.shippingLines?.isEmpty() == true)

        // when
        sut.onUpdatedShipping(result)

        // then
        assertThat(orderDraft?.shippingLines).hasSize(1)
    }

    @Test
    fun `when removing a shipping fee, then mark the first one with null methodId`() {
        // given
        val itemId = 1L
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        shippingLines = listOf(
                            Order.ShippingLine(itemId, "first", "first", BigDecimal(1), BigDecimal.ZERO),
                            Order.ShippingLine(2L, "second", "second", BigDecimal(2), BigDecimal.ZERO),
                            Order.ShippingLine(3L, "third", "third", BigDecimal(3), BigDecimal.ZERO),
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
        sut.onRemoveShipping(itemId)

        val item = orderDraft?.shippingLines?.find { it.itemId == itemId }

        // then
        assertNotNull(item)
        assertThat(item.methodId).isNull()
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
    fun `should initialize with empty order`() {
        sut.orderDraft.observeForever {}

        assertThat(sut.orderDraft.value)
            .usingRecursiveComparison()
            .ignoringFields("dateCreated", "dateModified")
            .isEqualTo(Order.getEmptyOrder(Date(), Date()))
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

        val productCount = sut.products.value?.count() ?: 0

        verify(tracker).track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                KEY_HORIZONTAL_SIZE_CLASS to VALUE_DEVICE_TYPE_COMPACT,
                KEY_STATUS to defaultOrderValue.status,
                KEY_PRODUCT_COUNT to productCount,
                KEY_HAS_CUSTOMER_DETAILS to defaultOrderValue.billingAddress.hasInfo(),
                KEY_HAS_FEES to defaultOrderValue.feesLines.isNotEmpty(),
                KEY_HAS_SHIPPING_METHOD to defaultOrderValue.shippingLines.isNotEmpty()
            )
        )
    }

    @Test
    fun `when coupon added should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponAdded("abc")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_ADD,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )
    }

    @Test
    fun `when coupon removed should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        val couponEditResult = OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon("abc")
        sut.onCouponEditResult(couponEditResult)

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_REMOVE,
            mapOf(KEY_FLOW to VALUE_FLOW_CREATION)
        )
    }

    @Test
    fun `given coupon code rejected by backend, then should display message`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn
                flowOf(
                    Failed(
                        WooException(
                            WooError(
                                WooErrorType.INVALID_COUPON,
                                BaseRequest.GenericErrorType.UNKNOWN
                            )
                        )
                    )
                )
        }
        createSut()
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCouponAdded("abc")

        with(lastReceivedEvent) {
            this == OnCouponRejectedByBackend
        }
    }

    @Test
    fun `given sku, when view model init, then fetch product information`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "123",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
    fun `given sku, when view model init, then display progress indicator`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "123",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
            var isUpdatingOrderDraft: Boolean? = null
            sut.viewStateData.observeForever { _, viewState ->
                isUpdatingOrderDraft = viewState.isUpdatingOrderDraft
            }

            assertTrue(isUpdatingOrderDraft!!)
        }
    }

    @Test
    fun `given empty sku, when view model init, then do not fetch product information`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "",
                null,
            ).toSavedStateHandle()
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

    @Test
    fun `given scanning initiated from the order list screen, when product search via sku succeeds, then track event with proper source`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "12345",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        productType = "variable-subscription",
                    )
                )
            )

            createSut(navArgs)

            verify(tracker).track(
                AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_SUCCESS,
                mapOf(
                    AnalyticsTracker.KEY_SCANNING_SOURCE to "order_list",
                    KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given scanning initiated from the order list screen, when product search via sku fails, then track event with proper source`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "12345",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(null)

            createSut(navArgs)

            verify(tracker).track(
                AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    AnalyticsTracker.KEY_SCANNING_SOURCE to "order_list",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatUPCA.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Product search via SKU API call failed"
                )
            )
        }
    }

    @Test
    fun `given scanning initiated from the order list screen, when product search via sku succeeds but contains no product, then track event with proper source`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "12345",
                BarcodeFormat.FormatQRCode,
            ).toSavedStateHandle()
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
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(emptyList())

            createSut(navArgs)

            verify(tracker).track(
                AnalyticsEvent.PRODUCT_SEARCH_VIA_SKU_FAILURE,
                mapOf(
                    AnalyticsTracker.KEY_SCANNING_SOURCE to "order_list",
                    KEY_SCANNING_BARCODE_FORMAT to BarcodeFormat.FormatQRCode.formatName,
                    KEY_SCANNING_FAILURE_REASON to "Empty data response (no product found for the SKU)"
                )
            )
        }
    }

    @Test
    fun `given variable product from order list screen, when product added via scanning, then track correct source`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "12345",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                        parentID = 1L,
                        isVariable = true
                    )
                )
            )

            createSut(navArgs)

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    KEY_FLOW to VALUE_FLOW_CREATION,
                    KEY_PRODUCT_COUNT to 1,
                    AnalyticsTracker.KEY_SCANNING_SOURCE to ScanningSource.ORDER_LIST.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                    KEY_HAS_BUNDLE_CONFIGURATION to false,
                    KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given non-variable product from order list screen, when product added via scanning, then track correct source`() {
        testBlocking {
            val navArgs = OrderCreateEditFormFragmentArgs(
                Creation(),
                "12345",
                BarcodeFormat.FormatUPCA,
            ).toSavedStateHandle()
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
            whenever(
                productListRepository.searchProductList(
                    "12345",
                    WCProductStore.SkuSearchOptions.ExactSearch
                )
            ).thenReturn(
                listOf(
                    ProductTestUtils.generateProduct(
                        productId = 10L,
                    )
                )
            )

            createSut(navArgs)

            verify(tracker).track(
                AnalyticsEvent.ORDER_PRODUCT_ADD,
                mapOf(
                    KEY_FLOW to VALUE_FLOW_CREATION,
                    KEY_PRODUCT_COUNT to 1,
                    AnalyticsTracker.KEY_SCANNING_SOURCE to ScanningSource.ORDER_LIST.source,
                    KEY_PRODUCT_ADDED_VIA to ProductAddedVia.SCANNING.addedVia,
                    KEY_HAS_BUNDLE_CONFIGURATION to false,
                    KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    // region Custom Amounts
    @Test
    fun `when custom amount added, then fee line gets updated`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.size).isEqualTo(1)
    }

    @Test
    fun `when custom amount added, then disable the custom amount section until the operation is complete`() {
        var viewState: MutableList<ViewState> = mutableListOf()
        sut.viewStateData.liveData.observeForever {
            viewState.add(it)
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        // The first state will be triggered for updating the progress, hence we need to verify the second state
        assertFalse(viewState[1].isEditable)
    }

    @Test
    fun `when custom amount added, then enable the custom amount section after the operation is complete`() {
        var viewState: MutableList<ViewState> = mutableListOf()
        sut.viewStateData.liveData.observeForever {
            viewState.add(it)
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        // The first state will be triggered for updating the progress, second state for disabling the custom amount. Hence we need to verify the third state
        assertTrue(viewState[2].isEditable)
    }

    @Test
    fun `when custom amount added with tax status as taxable, then fee line gets updated with proper tax status`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = true),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.first()?.taxStatus).isEqualTo(Order.FeeLine.FeeLineTaxStatus.TAXABLE)
    }

    @Test
    fun `when custom amount added with tax status as false, then fee line gets updated with proper tax status`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = false),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.first()?.taxStatus).isEqualTo(Order.FeeLine.FeeLineTaxStatus.NONE)
    }

    @Test
    fun `when custom amount added, then fee line gets updated with proper amount`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.total).isEqualTo(BigDecimal.TEN)
    }

    @Test
    fun `when custom amount added, then fee line gets updated with proper name`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.name).isEqualTo("Test amount")
    }

    @Test
    fun `when custom amount added without name, then fee line gets updated with default name`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.name).isEqualTo(CUSTOM_AMOUNT)
    }

    @Test
    fun `when custom amount is updated, then fee line gets updated`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        val updatedCustomAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.ONE,
            name = "Test amount updated",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)
        assertThat(orderDraft?.feesLines?.size).isEqualTo(1)

        sut.onCustomAmountUpsert(updatedCustomAmountUIModel)

        assertThat(orderDraft?.feesLines?.size).isEqualTo(1)
    }

    @Test
    fun `when custom amount is updated with amount, then fee line gets updated`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        val updatedCustomAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.ONE,
            name = "Test amount updated",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)

        sut.onCustomAmountUpsert(updatedCustomAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.total).isEqualTo(BigDecimal.ONE)
    }

    @Test
    fun `when custom amount is updated with tax status, then fee line gets updated with proper tax status`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = false),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        val updatedCustomAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.ONE,
            name = "Test amount updated",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = true),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)

        sut.onCustomAmountUpsert(updatedCustomAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.taxStatus).isEqualTo(Order.FeeLine.FeeLineTaxStatus.TAXABLE)
    }

    @Test
    fun `when custom amount is updated with name, then fee line gets updated`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertThat(orderDraft?.feesLines?.size).isEqualTo(0)
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        val updatedCustomAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.ONE,
            name = "Test amount updated",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)

        sut.onCustomAmountUpsert(updatedCustomAmountUIModel)

        assertThat(orderDraft?.feesLines?.firstOrNull()?.name).isEqualTo("Test amount updated")
    }

    @Test
    fun `when custom amount removed, then fee line is updated`() {
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)
        assertThat(orderDraft?.feesLines?.size).isEqualTo(1)

        sut.onCustomAmountRemoved(
            CustomAmountUIModel(
                id = 0L,
                amount = BigDecimal.TEN,
                name = "Test amount",
                type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
            )
        )

        assertThat(orderDraft?.feesLines?.filter { it.name != null }?.size).isEqualTo(0)
    }

    @Test
    fun `when custom amount removed, then disable the custom amount section until the operation is complete`() {
        var viewState: MutableList<ViewState> = mutableListOf()
        sut.viewStateData.liveData.observeForever {
            viewState.add(it)
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)

        sut.onCustomAmountRemoved(
            CustomAmountUIModel(
                id = 0L,
                amount = BigDecimal.TEN,
                name = "Test amount",
                type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
            )
        )

        assertFalse(viewState[viewState.size - 2].isEditable)
    }

    @Test
    fun `when custom amount removed, then enable the custom amount section after the operation is complete`() {
        var viewState: MutableList<ViewState> = mutableListOf()
        sut.viewStateData.liveData.observeForever {
            viewState.add(it)
        }
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )
        sut.onCustomAmountUpsert(customAmountUIModel)

        sut.onCustomAmountRemoved(
            CustomAmountUIModel(
                id = 0L,
                amount = BigDecimal.TEN,
                name = "Test amount",
                type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
            )
        )

        assertTrue(viewState[viewState.size - 1].isEditable)
    }

    @Test
    fun `given totals helper returns minimised, when totals checked, then return minimised`() {
        testBlocking {
            val totalsSectionsState = mock<TotalsSectionsState.Minimised>()
            whenever(
                totalsHelper.mapToPaymentTotalsState(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(totalsSectionsState)

            var totalsData: TotalsSectionsState? = null

            sut.totalsData.observeForever {
                totalsData = it
            }

            createSut()

            assertThat(totalsData).isEqualTo(totalsSectionsState)
        }
    }

    @Test
    fun `given totals helper returns full, when totals checked, then return full`() {
        testBlocking {
            val totalsSectionsState = mock<TotalsSectionsState.Full>()
            whenever(
                totalsHelper.mapToPaymentTotalsState(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(totalsSectionsState)
            var totalsData: TotalsSectionsState? = null

            sut.totalsData.observeForever {
                totalsData = it
            }

            createSut()

            assertThat(totalsData).isEqualTo(totalsSectionsState)
        }
    }

    @Test
    fun `given totals helper returns full, when expand collapse clicked, then ORDER_FORM_TOTALS_PANEL_TOGGLED tracked with false and true`() {
        testBlocking {
            val totalsSectionsState = mock<TotalsSectionsState.Full>()
            val onExpandCollapseClickedCaptor = argumentCaptor<() -> Unit>()
            whenever(
                totalsHelper.mapToPaymentTotalsState(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    onExpandCollapseClickedCaptor.capture(),
                    any(),
                )
            ).thenReturn(totalsSectionsState)

            sut.totalsData.observeForever { }

            createSut()

            onExpandCollapseClickedCaptor.firstValue.invoke()
            onExpandCollapseClickedCaptor.firstValue.invoke()

            verify(tracker).track(
                AnalyticsEvent.ORDER_FORM_TOTALS_PANEL_TOGGLED,
                mapOf(
                    KEY_FLOW to VALUE_FLOW_CREATION,
                    KEY_EXPANDED to false,
                    KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
            verify(tracker).track(
                AnalyticsEvent.ORDER_FORM_TOTALS_PANEL_TOGGLED,
                mapOf(
                    KEY_FLOW to VALUE_FLOW_CREATION,
                    KEY_EXPANDED to true,
                    KEY_HORIZONTAL_SIZE_CLASS to "compact"
                )
            )
        }
    }

    @Test
    fun `given totals helper returns full, when height changed, then event OnTotalsSectionHeightChanged with height emitted`() {
        testBlocking {
            val totalsSectionsState = mock<TotalsSectionsState.Full>()
            val onHeightChangedCaptor = argumentCaptor<(Int) -> Unit>()
            whenever(
                totalsHelper.mapToPaymentTotalsState(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    onHeightChangedCaptor.capture(),
                )
            ).thenReturn(totalsSectionsState)

            var lastReceivedEvent: Event? = null
            sut.event.observeForever {
                lastReceivedEvent = it
            }

            sut.totalsData.observeForever { }

            createSut()

            onHeightChangedCaptor.firstValue.invoke(100)
            assertThat(lastReceivedEvent).isEqualTo(OnTotalsSectionHeightChanged(100))
        }
    }

    @Test
    fun `given totals helper returns full and creation, when main button clicked, then PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED tracked`() {
        testBlocking {
            val totalsSectionsState = mock<TotalsSectionsState.Full>()
            val onMainButtonClickedCaptor = argumentCaptor<() -> Unit>()
            whenever(
                totalsHelper.mapToPaymentTotalsState(
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    onMainButtonClickedCaptor.capture(),
                    any(),
                    any(),
                    any(),
                )
            ).thenReturn(totalsSectionsState)

            sut.totalsData.observeForever { }

            createSut()

            onMainButtonClickedCaptor.firstValue.invoke()

            verify(tracker).track(
                AnalyticsEvent.PAYMENTS_FLOW_ORDER_COLLECT_PAYMENT_TAPPED,
                mapOf(
                    KEY_HORIZONTAL_SIZE_CLASS to VALUE_DEVICE_TYPE_COMPACT,
                    KEY_STATUS to Order.Status.Pending,
                    KEY_PRODUCT_COUNT to 0,
                    KEY_HAS_CUSTOMER_DETAILS to false,
                    KEY_HAS_FEES to false,
                    KEY_HAS_SHIPPING_METHOD to false,
                    KEY_FLOW to VALUE_FLOW_CREATION
                )
            )
        }
    }

    @Test
    fun `when custom amount is added, then proper event is tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(ADD_CUSTOM_AMOUNT_DONE_TAPPED, mapOf(KEY_HORIZONTAL_SIZE_CLASS to "compact"))
    }

    @Test
    fun `when custom amount is updated, then ADD_CUSTOM_AMOUNT_DONE_TAPPED event is tracked`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        feesLines = listOf(
                            Order.FeeLine.EMPTY.copy(
                                id = 1,
                                total = BigDecimal(1),
                                name = "Test amount",
                            ),
                        )
                    )
                )
            )
        }
        createSut()
        val customAmountUIModel = CustomAmountUIModel(
            id = 1L,
            amount = BigDecimal.ONE,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(ADD_CUSTOM_AMOUNT_DONE_TAPPED, mapOf(KEY_HORIZONTAL_SIZE_CLASS to "compact"))
    }

    @Test
    fun `when custom amount is updated, then do not track order_fee_add event`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        feesLines = listOf(
                            Order.FeeLine.EMPTY.copy(
                                id = 1,
                                total = BigDecimal(1),
                                name = "Test amount",
                            ),
                        )
                    )
                )
            )
        }
        createSut()
        val customAmountUIModel = CustomAmountUIModel(
            id = 1L,
            amount = BigDecimal.ONE,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker, never()).track(
            ORDER_FEE_ADD,
            mapOf(
                KEY_FLOW to VALUE_FLOW_CREATION,
                KEY_CUSTOM_AMOUNT_TAX_STATUS to "none"
            )
        )
    }

    @Test
    fun `when custom amount is updated, then track order_fee_update event`() {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(
                Succeeded(
                    Order.getEmptyOrder(Date(), Date()).copy(
                        feesLines = listOf(
                            Order.FeeLine.EMPTY.copy(
                                id = 1,
                                total = BigDecimal(1),
                                name = "Test amount",
                            ),
                        )
                    )
                )
            )
        }
        createSut()
        val customAmountUIModel = CustomAmountUIModel(
            id = 1L,
            amount = BigDecimal.ONE,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(
            ORDER_FEE_UPDATE,
            mapOf(
                KEY_FLOW to VALUE_FLOW_CREATION,
                KEY_CUSTOM_AMOUNT_TAX_STATUS to "none",
                KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when custom amount name is added, then proper event is tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(ADD_CUSTOM_AMOUNT_NAME_ADDED)
    }

    @Test
    fun `when custom amount name is added, then fee add event is tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(
            eq(ORDER_FEE_ADD),
            any()
        )
    }

    @Test
    fun `when custom amount name is added with tax, then fee add event is tracked with tax_status value taxable`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = true),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(
            ORDER_FEE_ADD,
            mapOf(
                KEY_FLOW to VALUE_FLOW_CREATION,
                KEY_CUSTOM_AMOUNT_TAX_STATUS to VALUE_CUSTOM_AMOUNT_TAX_STATUS_TAXABLE,
                KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when custom amount name is added with no tax, then fee add event is tracked with tax_status value none`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = false),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(
            ORDER_FEE_ADD,
            mapOf(
                KEY_FLOW to VALUE_FLOW_CREATION,
                KEY_CUSTOM_AMOUNT_TAX_STATUS to VALUE_CUSTOM_AMOUNT_TAX_STATUS_NONE,
                KEY_HORIZONTAL_SIZE_CLASS to "compact"
            )
        )
    }

    @Test
    fun `when custom amount name is not added, then event is not tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Custom Amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker, never()).track(ADD_CUSTOM_AMOUNT_NAME_ADDED)
    }

    @Test
    fun `when custom amount name is added based on percentage, then percentage add event is tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = false),
            type = CustomAmountsViewModel.CustomAmountType.PERCENTAGE_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker).track(ADD_CUSTOM_AMOUNT_PERCENTAGE_ADDED)
    }

    @Test
    fun `when custom amount name is added based on fixed amount, then percentage add event is not tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Test amount",
            taxStatus = CustomAmountsViewModel.TaxStatus(isTaxable = false),
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountUpsert(customAmountUIModel)

        verify(tracker, never()).track(ADD_CUSTOM_AMOUNT_PERCENTAGE_ADDED)
    }

    @Test
    fun `when custom amount is removed, then event is tracked`() {
        val customAmountUIModel = CustomAmountUIModel(
            id = 0L,
            amount = BigDecimal.TEN,
            name = "Custom Amount",
            type = CustomAmountsViewModel.CustomAmountType.FIXED_CUSTOM_AMOUNT
        )

        sut.onCustomAmountRemoved(customAmountUIModel)

        verify(tracker).track(ORDER_CREATION_REMOVE_CUSTOM_AMOUNT_TAPPED, mapOf(KEY_HORIZONTAL_SIZE_CLASS to "compact"))
    }
    //endregion
}
