package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_COMPACT
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_DEVICE_TYPE_REGULAR
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_EDITING
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Edit
import com.woocommerce.android.ui.orders.creation.coupon.edit.OrderCreateCouponDetailsViewModel
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
import com.woocommerce.android.ui.orders.creation.taxes.TaxBasedOnSetting
import com.woocommerce.android.ui.orders.creation.views.ProductAmountEvent
import com.woocommerce.android.ui.products.ProductType
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.stub
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.util.Date

@ExperimentalCoroutinesApi
// Remove Silent runner when feature is completed
@RunWith(MockitoJUnitRunner.Silent::class)
class EditFocusedOrderCreateEditViewModelTest : UnifiedOrderEditViewModelTest() {
    override val mode: Mode = Edit(defaultOrderValue.id)
    override val sku: String = "123"
    override val barcodeFormat: BarcodeFormat = BarcodeFormat.FormatUPCA
    override val tracksFlow: String = VALUE_FLOW_EDITING

    override fun initMocksForAnalyticsWithOrder(order: Order) {
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        orderDetailRepository = mock {
            onBlocking { getOrderById(order.id) }.doReturn(order)
        }
    }

    @Test
    fun `should load order from repository`() = testBlocking {
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(defaultOrderValue)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(defaultOrderValue))
        }

        createSut()

        var orderDraft: Order? = null

        sut.orderDraft.observeForever { new ->
            orderDraft = new
        }

        assertThat(orderDraft).isEqualTo(defaultOrderValue)
    }

    @Test
    fun `when hitting the back button, then close the screen`() {
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(defaultOrderValue)
        }
        createSut()
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onBackButtonClicked()

        assertThat(lastReceivedEvent).isEqualTo(Exit)
    }

    @Test
    fun `when confirming order edit, then dismiss the screen`() {
        var lastReceivedEvent: Event? = null
        sut.event.observeForever {
            lastReceivedEvent = it
        }

        sut.onCreateOrderClicked(defaultOrderValue)

        assertThat(lastReceivedEvent).isEqualTo(Exit)
    }

    @Test
    fun `when isEditable is true on the edit flow the order is editable`() {
        val order = defaultOrderValue.copy(isEditable = true)
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(true)
    }

    @Test
    fun `when isEditable is false on the edit flow the order is NOT editable`() {
        val order = defaultOrderValue.copy(isEditable = false)
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        assertThat(lastReceivedState?.isEditable).isEqualTo(false)
    }

    @Test
    fun `when done button tapped, then don't send the track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCreateOrderClicked(defaultOrderValue)

        verify(tracker, never()).track(
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
    fun `given phone, when done button tapped, then track event with correct properties`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCreateOrderClicked(defaultOrderValue)

        verify(tracker, never()).track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to VALUE_DEVICE_TYPE_COMPACT,
                AnalyticsTracker.KEY_STATUS to defaultOrderValue.status,
                AnalyticsTracker.KEY_PRODUCT_COUNT to sut.products.value?.count(),
                AnalyticsTracker.KEY_HAS_CUSTOMER_DETAILS to defaultOrderValue.billingAddress.hasInfo(),
                AnalyticsTracker.KEY_HAS_FEES to defaultOrderValue.feesLines.isNotEmpty(),
                AnalyticsTracker.KEY_HAS_SHIPPING_METHOD to defaultOrderValue.shippingLines.isNotEmpty()
            )
        )
    }

    @Test
    fun `given tablet, when done button tapped, then track event with correct properties`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCreateOrderClicked(defaultOrderValue)

        verify(tracker, never()).track(
            AnalyticsEvent.ORDER_CREATE_BUTTON_TAPPED,
            mapOf(
                AnalyticsTracker.KEY_HORIZONTAL_SIZE_CLASS to VALUE_DEVICE_TYPE_REGULAR,
                AnalyticsTracker.KEY_STATUS to defaultOrderValue.status,
                AnalyticsTracker.KEY_PRODUCT_COUNT to sut.products.value?.count(),
                AnalyticsTracker.KEY_HAS_CUSTOMER_DETAILS to defaultOrderValue.billingAddress.hasInfo(),
                AnalyticsTracker.KEY_HAS_FEES to defaultOrderValue.feesLines.isNotEmpty(),
                AnalyticsTracker.KEY_HAS_SHIPPING_METHOD to defaultOrderValue.shippingLines.isNotEmpty()
            )
        )
    }

    @Test
    fun `when new non-empty coupon added, then should update coupon lines in order draft`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestOrderDraft: Order? = null
        sut.orderDraft.observeForever {
            latestOrderDraft = it
        }

        sut.onCouponAdded("new_code")

        latestOrderDraft!!.couponLines.filter { it.code == "new_code" }.apply {
            assertTrue(isNotEmpty())
            assertEquals(1, size)
        }
    }

    @Test
    fun `given order with non-empty coupon, when coupon removed, then should remove coupon from order`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestOrderDraft: Order? = null
        sut.orderDraft.observeForever {
            latestOrderDraft = it
        }

        sut.onCouponAdded("new_code")
        latestOrderDraft!!.couponLines.filter { it.code == "new_code" }.apply {
            assertTrue(isNotEmpty())
            assertEquals(1, size)
        }

        // when
        val couponRemoveResult = OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon("new_code")
        sut.onCouponEditResult(couponRemoveResult)

        // then
        latestOrderDraft!!.couponLines.apply { assertTrue(isEmpty()) }
    }

    @Test
    fun `given order with multiple coupons, when one coupon removed, then the other one should remain in the order`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestOrderDraft: Order? = null
        sut.orderDraft.observeForever {
            latestOrderDraft = it
        }
        sut.onCouponAdded("new_code")
        sut.onCouponAdded("new_code2")

        // when
        sut.onCouponEditResult(OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon("new_code"))

        // then
        latestOrderDraft!!.couponLines.apply {
            assertTrue(any { it.code == "new_code2" })
        }
    }

    @Test
    fun `given no coupon added to order when add new coupon clicked, then should redirect to coupon form`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestEvent: Event? = null
        sut.event.observeForever {
            latestEvent = it
        }

        sut.onCouponButtonClicked()

        assertEquals(OrderCreateEditNavigationTarget.EditCoupon(sut.mode, null), latestEvent)
    }

    @Test
    fun `given coupon line present in order, when coupon button clicked, then should redirect to coupon list screen`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestEvent: Event? = null
        sut.event.observeForever {
            latestEvent = it
        }
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        sut.onCouponAdded("new_code")

        // when
        sut.onCouponButtonClicked()

        // then
        assertEquals(OrderCreateEditNavigationTarget.CouponList(sut.mode, orderDraft!!.couponLines), latestEvent)
    }

    @Test
    fun `given no items in order, then add coupon button should be hidden`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        assertFalse(sut.viewStateData.liveData.value!!.isCouponButtonEnabled)
    }

    @Test
    fun `given editable order and containing at least one item, then add coupon button should be enabled`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(
                Order.Item(
                    1L,
                    1L,
                    "name",
                    BigDecimal(1),
                    "",
                    1f,
                    BigDecimal(1),
                    BigDecimal(1),
                    BigDecimal(1),
                    1L,
                    listOf()
                )
            )
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }

        assertTrue(lastReceivedState!!.isCouponButtonEnabled)
    }

    @Test
    fun `given coupon applied to order, then should disable adding discount to a product`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(
                Order.Item(
                    1L,
                    1L,
                    "name",
                    BigDecimal(1),
                    "",
                    1f,
                    BigDecimal(1),
                    BigDecimal(1),
                    BigDecimal(1),
                    1L,
                    listOf()
                )
            ),
            couponLines = listOf(Order.CouponLine("code", 1L, ""))
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertTrue(orderDraft!!.couponLines.isNotEmpty())
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        // then
        assertFalse(lastReceivedState!!.areDiscountButtonsEnabled)
    }

    @Test
    fun `given a coupon applied to order, then should allow adding another one`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(
                Order.Item(
                    1L,
                    1L,
                    "name",
                    BigDecimal(1),
                    "",
                    1f,
                    BigDecimal(1),
                    BigDecimal(1),
                    BigDecimal(1),
                    1L,
                    listOf()
                )
            ),
            couponLines = listOf(Order.CouponLine("code", 1L, ""))
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertTrue(orderDraft!!.couponLines.isNotEmpty())
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        // then
        assertTrue(lastReceivedState!!.isCouponButtonEnabled)
    }

    @Test
    fun `given no coupons applied to order, then should enable adding discount to a product`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(
                Order.Item(
                    1L,
                    1L,
                    "name",
                    BigDecimal(1),
                    "",
                    1f,
                    BigDecimal(1),
                    BigDecimal(1),
                    BigDecimal(1),
                    1L,
                    listOf()
                )
            ),
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }
        assertTrue(orderDraft!!.couponLines.isEmpty())
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        // then
        assertTrue(lastReceivedState!!.areDiscountButtonsEnabled)
    }

    @Test
    fun `given discount applied to at least one item, then should disable adding coupon to the order`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val item = Order.Item(
            itemId = 1L,
            productId = 1L,
            name = "name",
            price = BigDecimal(1),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal(10),
            totalTax = BigDecimal(5),
            total = BigDecimal(1),
            variationId = 1L,
            attributesList = listOf()
        )
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(item),
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        // then
        assertTrue(item.discount > BigDecimal.ZERO)
        assertFalse(lastReceivedState!!.isCouponButtonEnabled)
    }

    @Test
    fun `given no discounts applied to order items, then should disable adding coupon to the order`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val item = Order.Item(
            itemId = 1L,
            productId = 1L,
            name = "name",
            price = BigDecimal(1),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal(10),
            totalTax = BigDecimal(1),
            total = BigDecimal(10),
            variationId = 1L,
            attributesList = listOf()
        )
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(item),
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }
        // then
        assertTrue(item.discount == BigDecimal.ZERO)
        assertTrue(lastReceivedState!!.isCouponButtonEnabled)
    }

    @Test
    fun `given editable order and order paid, then set tax rate button should be disabled`() {
        testBlocking {
            initMocksForAnalyticsWithOrder(defaultOrderValue)
            val order = defaultOrderValue.copy(
                datePaid = Date()
            )
            whenever(orderCreateEditRepository.fetchTaxBasedOnSetting()).thenReturn(TaxBasedOnSetting.BillingAddress)

            orderDetailRepository.stub {
                onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
            }
            createUpdateOrderUseCase = mock {
                onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
            }

            createSut()

            assertFalse(sut.viewStateData.liveData.value!!.taxRateSelectorButtonState.isShown)
        }
    }

    @Test
    fun `given editable order and order not paid, then set tax rate button should be enabled`() {
        testBlocking {
            initMocksForAnalyticsWithOrder(defaultOrderValue)
            val order = defaultOrderValue.copy(
                datePaid = null
            )
            whenever(orderCreateEditRepository.fetchTaxBasedOnSetting()).thenReturn(TaxBasedOnSetting.BillingAddress)

            whenever(resourceProvider.getString(any())).thenReturn("label")

            orderDetailRepository.stub {
                onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
            }
            createUpdateOrderUseCase = mock {
                onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
            }

            createSut()

            assertTrue(sut.viewStateData.liveData.value!!.taxRateSelectorButtonState.isShown)
        }
    }

    @Test
    fun `given not editable order and containing at least one item, then add coupon button should be disabled`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val order = defaultOrderValue.copy(
            isEditable = false,
            items = listOf(
                Order.Item(
                    1L,
                    1L,
                    "name",
                    BigDecimal(1),
                    "",
                    1f,
                    BigDecimal(1),
                    BigDecimal(1),
                    BigDecimal(1),
                    1L,
                    listOf()
                )
            )
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()
        var lastReceivedState: OrderCreateEditViewModel.ViewState? = null
        sut.viewStateData.liveData.observeForever {
            lastReceivedState = it
        }

        assertFalse(lastReceivedState!!.isCouponButtonEnabled)
    }

    @Test
    fun `when coupon added, then should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponAdded("code ")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_EDITING)
        )
    }

    @Test
    fun `when coupon removed, then should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponEditResult(OrderCreateCouponDetailsViewModel.CouponEditResult.RemoveCoupon("abc"))

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_EDITING)
        )
    }

    @Test
    fun `given 100_001 quantity, when onItemAmountChanged with increase, then do nothing`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        val item = Order.Item(
            1L,
            1L,
            "name",
            BigDecimal(1),
            "",
            1f,
            BigDecimal(1),
            BigDecimal(1),
            BigDecimal(1),
            1L,
            listOf()
        )
        val order = defaultOrderValue.copy(
            isEditable = true,
            items = listOf(item)
        )
        orderDetailRepository.stub {
            onBlocking { getOrderById(defaultOrderValue.id) }.doReturn(order)
        }
        createUpdateOrderUseCase = mock {
            onBlocking { invoke(any(), any()) } doReturn flowOf(Succeeded(order))
        }
        createSut()

        var orderDraft: Order? = null
        sut.orderDraft.observeForever {
            orderDraft = it
        }

        val productInfo: ProductInfo = mock {
            on { productType }.thenReturn(ProductType.SIMPLE)
        }
        sut.onItemAmountChanged(
            OrderCreationProduct.ProductItem(
                item = item,
                productInfo = productInfo
            ),
            ProductAmountEvent.Change("100000")
        )

        sut.onItemAmountChanged(
            OrderCreationProduct.ProductItem(
                item = item.copy(quantity = 100_000f),
                productInfo = productInfo
            ),
            ProductAmountEvent.Increase
        )

        assertThat(orderDraft!!.items[0].quantity).isEqualTo(100_000f)
    }
}
