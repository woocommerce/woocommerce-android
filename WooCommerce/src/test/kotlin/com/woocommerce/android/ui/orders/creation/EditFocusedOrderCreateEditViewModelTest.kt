package com.woocommerce.android.ui.orders.creation

import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion.VALUE_FLOW_EDITING
import com.woocommerce.android.model.Order
import com.woocommerce.android.ui.orders.creation.CreateUpdateOrder.OrderUpdateStatus.Succeeded
import com.woocommerce.android.ui.orders.creation.GoogleBarcodeFormatMapper.BarcodeFormat
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode
import com.woocommerce.android.ui.orders.creation.OrderCreateEditViewModel.Mode.Edit
import com.woocommerce.android.ui.orders.creation.navigation.OrderCreateEditNavigationTarget
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
    fun `when done button tapped, don't send the track event`() {
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
    fun `when new non-empty coupon added then should update coupon lines in order draft`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestOrderDraft: Order? = null
        sut.orderDraft.observeForever {
            latestOrderDraft = it
        }

        sut.onCouponEntered("new_code")

        latestOrderDraft!!.couponLines.filter { it.code == "new_code" }.apply {
            assertTrue(isNotEmpty())
            assertEquals(1, size)
        }
    }

    @Test
    fun `given order with non-empty coupon when empty coupon added then should remove coupon lines from order draft`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestOrderDraft: Order? = null
        sut.orderDraft.observeForever {
            latestOrderDraft = it
        }
        sut.onCouponEntered("new_code")
        latestOrderDraft!!.couponLines.filter { it.code == "new_code" }.apply {
            assertTrue(isNotEmpty())
            assertEquals(1, size)
        }

        // when
        sut.onCouponEntered("")

        // then
        latestOrderDraft!!.couponLines.apply {
            assertTrue(isEmpty())
        }
    }

    @Test
    fun `given no coupon added to order when add new coupon clicked should redirect to coupon form`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        var latestEvent: Event? = null
        sut.event.observeForever {
            latestEvent = it
        }

        sut.onCouponButtonClicked()

        assertEquals(OrderCreateEditNavigationTarget.EditCoupon(null), latestEvent)
    }

    @Test
    fun `given coupon line present in order when add new coupon clicked should redirect to coupon form with coupon code`() {
        // given
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        sut.onCouponEntered("code")
        var latestEvent: Event? = null
        sut.event.observeForever {
            latestEvent = it
        }

        // when
        sut.onCouponButtonClicked()

        // then
        assertEquals(OrderCreateEditNavigationTarget.EditCoupon("code"), latestEvent)
    }

    @Test
    fun `given no items in order then add coupon button should be hidden`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()
        assertFalse(sut.viewStateData.liveData.value!!.isCouponButtonEnabled)
    }

    @Test
    fun `when coupon added should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponEntered("code")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_ADD,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_EDITING)
        )
    }

    @Test
    fun `when coupon removed should track event`() {
        initMocksForAnalyticsWithOrder(defaultOrderValue)
        createSut()

        sut.onCouponEntered("")

        verify(tracker).track(
            AnalyticsEvent.ORDER_COUPON_REMOVE,
            mapOf(AnalyticsTracker.KEY_FLOW to VALUE_FLOW_EDITING)
        )
    }
}
