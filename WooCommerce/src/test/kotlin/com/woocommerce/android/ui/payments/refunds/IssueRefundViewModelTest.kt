package com.woocommerce.android.ui.payments.refunds

import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.extensions.isEqualTo
import com.woocommerce.android.model.AmbiguousLocation
import com.woocommerce.android.model.Location
import com.woocommerce.android.model.Order
import com.woocommerce.android.model.OrderMapper
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.util.getOrAwaitValue
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import org.wordpress.android.fluxc.model.refunds.WCRefundModel.WCRefundItem
import org.wordpress.android.fluxc.persistence.entity.OrderEntity
import org.wordpress.android.fluxc.store.WCOrderStore
import org.wordpress.android.fluxc.store.WCRefundStore
import java.math.BigDecimal
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
class IssueRefundViewModelTest : BaseUnitTest() {
    companion object {
        private const val ORDER_ID = 1L
    }

    private val orderStore: WCOrderStore = mock()
    private val selectedSite: SelectedSite = mock()
    private val refundStore: WCRefundStore = mock {
        on { getAllRefunds(any(), any()) } doReturn emptyList()
    }
    private val currencyFormatter: CurrencyFormatter = mock()
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments[0].toString() }
        on { getString(R.string.multiple_shipping) } doAnswer { "Multiple shipping lines" }
        on { getString(any(), any()) } doAnswer { i ->
            "You can refund " + i.arguments[1].toString()
        }
    }
    private val orderMapper = OrderMapper(
        getLocations = mock {
            on { invoke(any(), any()) } doReturn (Location.EMPTY to AmbiguousLocation.EMPTY)
        },
        mock()
    )

    private val savedState = IssueRefundFragmentArgs(ORDER_ID).toSavedStateHandle()

    private lateinit var viewModel: IssueRefundViewModel

    private fun initViewModel(
        orderMapper: OrderMapper = this.orderMapper
    ) {
        whenever(selectedSite.get()).thenReturn(SiteModel())
        whenever(currencyFormatter.buildBigDecimalFormatter(any())).thenReturn { "" }

        viewModel = IssueRefundViewModel(
            savedState = savedState,
            currencyFormatter = currencyFormatter,
            orderStore = orderStore,
            selectedSite = selectedSite,
            resourceProvider = resourceProvider,
            refundStore = refundStore,
            orderMapper = orderMapper,
            analyticsTrackerWrapper = analyticsTrackerWrapper,
        )
    }

    @Test
    fun `given order has only custom amt, when refund button clicked, then refund custom amount toggle is enabled`() {
        testBlocking {
            val orderWithCustomAmount = OrderTestUtils.generateOrderWithCustomAmount()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithCustomAmount)

            initViewModel()

            val viewState = viewModel.viewState.getOrAwaitValue()

            viewState.feesSection.isFeesRefundAvailable.let { assertTrue(it) }
            assertTrue(viewState.feesSection.isFeesMainSwitchChecked)
        }
    }

    @Test
    fun `when order has no shipping, then refund notice is not visible`() {
        testBlocking {
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(OrderTestUtils.generateOrder())

            initViewModel()

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertFalse(viewState.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has one shipping, then the notice not visible`() {
        testBlocking {
            val orderWithShipping = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithShipping)

            initViewModel()

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertFalse(viewState.isRefundNoticeVisible)
        }
    }

    @Test
    fun `when order has multiple shipping, multiple shipping are mentioned in the notice`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()

            val viewState = viewModel.viewState.getOrAwaitValue()

            assertTrue(viewState.isRefundNoticeVisible)
            assertEquals("You can refund multiple shipping lines", viewState.refundNotice)
        }
    }

    @Test
    fun `when next button is tapped from items, then verify proper tracks event is triggered `() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = emptyList()
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)

            initViewModel()
            viewModel.viewState.getOrAwaitValue()
            viewModel.onNextButtonTappedFromItems()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_NEXT_BUTTON_TAPPED,
                mapOf(
                    AnalyticsTracker.KEY_ORDER_ID to ORDER_ID
                )
            )
        }
    }

    @Test
    fun `when refund quantity is tapped, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = emptyList()
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(refundStore.getAllRefunds(any(), any())).thenReturn(emptyList())

            initViewModel()
            viewModel.viewState.getOrAwaitValue()
            viewModel.onRefundQuantityTapped(1L)

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_ITEM_QUANTITY_DIALOG_OPENED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to ORDER_ID)
            )
        }
    }

    @Test
    fun `when select button is tapped, then proper track event is triggered`() {
        testBlocking {
            val orderWithMultipleShipping = OrderTestUtils.generateOrderWithMultipleShippingLines().copy(
                paymentMethod = "cod",
                metaData = emptyList()
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderWithMultipleShipping)
            whenever(refundStore.getAllRefunds(any(), any())).thenReturn(emptyList())

            initViewModel()
            viewModel.viewState.getOrAwaitValue()
            viewModel.onSelectButtonTapped()

            verify(analyticsTrackerWrapper).track(
                AnalyticsEvent.CREATE_ORDER_REFUND_SELECT_ALL_ITEMS_BUTTON_TAPPED,
                mapOf(AnalyticsTracker.KEY_ORDER_ID to ORDER_ID)
            )
        }
    }

    @Test
    fun `given order with zero refunded items, when vm init, then all items are shown`() = testBlocking {
        // GIVEN
        val items = listOf(
            mock<Order.Item> {
                on { quantity }.thenReturn(1.0F)
            },
            mock {
                on { quantity }.thenReturn(1.0F)
            },
            mock {
                on { quantity }.thenReturn(1.0F)
            },
        )
        val order = mock<Order> {
            on { this.items }.thenReturn(items)
            on { this.currency }.thenReturn("USD")
            on { maxRefund } doReturn BigDecimal.TEN
        }
        val orderEntity = mock<OrderEntity>()
        val orderMapper = mock<OrderMapper> {
            on { toAppModel(orderEntity) }.thenReturn(order)
        }
        whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderEntity)
        whenever(refundStore.getAllRefunds(any(), any())).thenReturn(emptyList())

        // WHEN
        initViewModel(orderMapper)

        // THEN
        val viewState = viewModel.viewState.getOrAwaitValue()
        assertThat(viewState.productsSection.refundItems).hasSize(3)
    }

    @Test
    fun `given order with 1 refunded items, when vm init, then all items minus one are shown`() = testBlocking {
        // GIVEN
        val items = listOf(
            mock<Order.Item> {
                on { productId }.thenReturn(1L)
                on { itemId }.thenReturn(1L)
                on { quantity }.thenReturn(1.0F)
            },
            mock {
                on { productId }.thenReturn(2L)
                on { itemId }.thenReturn(2L)
                on { quantity }.thenReturn(1.0F)
            },
            mock {
                on { productId }.thenReturn(3L)
                on { itemId }.thenReturn(3L)
                on { quantity }.thenReturn(1.0F)
            },
        )
        val order = mock<Order> {
            on { this.items }.thenReturn(items)
            on { this.currency }.thenReturn("USD")
            on { maxRefund } doReturn BigDecimal.TEN
        }
        val orderEntity = mock<OrderEntity>()
        val orderMapper = mock<OrderMapper> {
            on { toAppModel(orderEntity) }.thenReturn(order)
        }
        val refundedItems = listOf(
            mock<WCRefundItem> {
                on { productId }.thenReturn(1L)
                on { quantity }.thenReturn(-1)
                on { subtotal }.thenReturn(BigDecimal.ZERO)
                on { total }.thenReturn(BigDecimal.ZERO)
                on { sku }.thenReturn("")
                on { price }.thenReturn(BigDecimal.ZERO)
                on { totalTax }.thenReturn(BigDecimal.ZERO)
                on { metaData }.thenReturn(null)
            }
        )
        val refund = WCRefundModel(
            id = 1L,
            dateCreated = Date(),
            amount = BigDecimal.ZERO,
            reason = "",
            automaticGatewayRefund = false,
            items = refundedItems,
            shippingLineItems = listOf(),
            feeLineItems = listOf()
        )
        whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderEntity)
        whenever(refundStore.getAllRefunds(any(), any())).thenReturn(listOf(refund))

        // WHEN
        initViewModel(orderMapper)

        // THEN
        val viewState = viewModel.viewState.getOrAwaitValue()
        assertThat(viewState.productsSection.refundItems).hasSize(2)
    }

    @Test
    @Suppress("LongMethod")
    fun `given order with 6 product each and 2 fully refunded, when vm init, then 1 product with 2 items shown`() =
        testBlocking {
            // GIVEN
            val items = listOf(
                mock<Order.Item> {
                    on { productId }.thenReturn(1L)
                    on { itemId }.thenReturn(1L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(2L)
                    on { itemId }.thenReturn(2L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(3L)
                    on { itemId }.thenReturn(3L)
                    on { quantity }.thenReturn(2.0F)
                },
            )
            val order = mock<Order> {
                on { this.items }.thenReturn(items)
                on { this.currency }.thenReturn("USD")
                on { maxRefund } doReturn BigDecimal.TEN
            }
            val orderEntity = mock<OrderEntity>()
            val orderMapper = mock<OrderMapper> {
                on { toAppModel(orderEntity) }.thenReturn(order)
            }
            val refundedItems = listOf(
                mock<WCRefundItem> {
                    on { productId }.thenReturn(1L)
                    on { quantity }.thenReturn(-2)
                    on { subtotal }.thenReturn(BigDecimal.ZERO)
                    on { total }.thenReturn(BigDecimal.ZERO)
                    on { sku }.thenReturn("")
                    on { price }.thenReturn(BigDecimal.ZERO)
                    on { totalTax }.thenReturn(BigDecimal.ZERO)
                    on { metaData }.thenReturn(null)
                },
                mock<WCRefundItem> {
                    on { productId }.thenReturn(2L)
                    on { quantity }.thenReturn(-2)
                    on { subtotal }.thenReturn(BigDecimal.ZERO)
                    on { total }.thenReturn(BigDecimal.ZERO)
                    on { sku }.thenReturn("")
                    on { price }.thenReturn(BigDecimal.ZERO)
                    on { totalTax }.thenReturn(BigDecimal.ZERO)
                    on { metaData }.thenReturn(null)
                },
            )
            val refund = WCRefundModel(
                id = 1L,
                dateCreated = Date(),
                amount = BigDecimal.ZERO,
                reason = "",
                automaticGatewayRefund = false,
                items = refundedItems,
                shippingLineItems = listOf(),
                feeLineItems = listOf()
            )
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderEntity)
            whenever(refundStore.getAllRefunds(any(), any())).thenReturn(listOf(refund))

            // WHEN
            initViewModel(orderMapper)

            // THEN
            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.productsSection.refundItems).hasSize(1)
            assertThat(viewState.productsSection.refundItems.first().maxQuantity).isEqualTo(2.0F)
        }

    @Test
    @Suppress("LongMethod")
    fun `given order with 5 product each and some fully refunded some partially, when vm init, then the rest are shown`() =
        testBlocking {
            // GIVEN
            val items = listOf(
                mock<Order.Item> {
                    on { productId }.thenReturn(1L)
                    on { itemId }.thenReturn(1L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(2L)
                    on { itemId }.thenReturn(2L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(3L)
                    on { itemId }.thenReturn(3L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(4L)
                    on { itemId }.thenReturn(4L)
                    on { quantity }.thenReturn(2.0F)
                },
                mock {
                    on { productId }.thenReturn(5L)
                    on { itemId }.thenReturn(5L)
                    on { quantity }.thenReturn(2.0F)
                },
            )
            val order = mock<Order> {
                on { this.items }.thenReturn(items)
                on { this.currency }.thenReturn("USD")
                on { maxRefund } doReturn BigDecimal.TEN
            }
            val orderEntity = mock<OrderEntity>()
            val orderMapper = mock<OrderMapper> {
                on { toAppModel(orderEntity) }.thenReturn(order)
            }
            val refundedItems = listOf(
                mock<WCRefundItem> {
                    on { productId }.thenReturn(1L)
                    on { quantity }.thenReturn(-2)
                    on { subtotal }.thenReturn(BigDecimal.ZERO)
                    on { total }.thenReturn(BigDecimal.ZERO)
                    on { sku }.thenReturn("")
                    on { price }.thenReturn(BigDecimal.ZERO)
                    on { totalTax }.thenReturn(BigDecimal.ZERO)
                    on { metaData }.thenReturn(null)
                },
                mock<WCRefundItem> {
                    on { productId }.thenReturn(2L)
                    on { quantity }.thenReturn(-1)
                    on { subtotal }.thenReturn(BigDecimal.ZERO)
                    on { total }.thenReturn(BigDecimal.ZERO)
                    on { sku }.thenReturn("")
                    on { price }.thenReturn(BigDecimal.ZERO)
                    on { totalTax }.thenReturn(BigDecimal.ZERO)
                    on { metaData }.thenReturn(null)
                },
                mock<WCRefundItem> {
                    on { productId }.thenReturn(3L)
                    on { quantity }.thenReturn(-2)
                    on { subtotal }.thenReturn(BigDecimal.ZERO)
                    on { total }.thenReturn(BigDecimal.ZERO)
                    on { sku }.thenReturn("")
                    on { price }.thenReturn(BigDecimal.ZERO)
                    on { totalTax }.thenReturn(BigDecimal.ZERO)
                    on { metaData }.thenReturn(null)
                },
            )

            val refund = WCRefundModel(
                id = 1L,
                dateCreated = Date(),
                amount = BigDecimal.ZERO,
                reason = "",
                automaticGatewayRefund = false,
                items = refundedItems,
                shippingLineItems = listOf(),
                feeLineItems = listOf()
            )

            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(orderEntity)
            whenever(refundStore.getAllRefunds(any(), any())).thenReturn(listOf(refund))

            // WHEN
            initViewModel(orderMapper)

            // THEN
            val viewState = viewModel.viewState.getOrAwaitValue()
            assertThat(viewState.productsSection.refundItems).hasSize(3)
            assertThat(viewState.productsSection.refundItems[0].maxQuantity).isEqualTo(1.0F)
            assertThat(viewState.productsSection.refundItems[1].maxQuantity).isEqualTo(2.0F)
            assertThat(viewState.productsSection.refundItems[2].maxQuantity).isEqualTo(2.0F)
        }

    @Test
    fun `when preparing refund for products, then pass correct tax rates`() {
        testBlocking {
            val order = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(order)

            initViewModel()
            viewModel.onSelectButtonTapped()

            val items = viewModel.viewState.getOrAwaitValue().productsSection.refundItems
            items.map { it.toDataModel() }.forEach { refundItem ->
                val lineItem = order.getLineItemList().first { it.id == refundItem.itemId }
                assertThat(refundItem.refundTax).allMatch { refundTaxItem ->
                    val tax = lineItem.taxes!!.first { it.rateId == refundTaxItem.taxRateId }
                    tax.rateId == refundTaxItem.taxRateId &&
                        tax.total.isEqualTo(refundTaxItem.refundTotal)
                }
            }
        }
    }

    @Test
    fun `when preparing refund for shipping, then pass correct tax rates`() {
        testBlocking {
            val order = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(order)

            initViewModel()
            viewModel.onSelectButtonTapped()

            val shippingLines = viewModel.viewState.getOrAwaitValue()
                .shippingSection
                .shippingRefundLines
            shippingLines.map { it.toDataModel() }.forEach { refundItem ->
                val shippingLine = order.getShippingLineList().first { it.id == refundItem.itemId }
                assertThat(refundItem.refundTax).allMatch { refundTaxItem ->
                    val tax = shippingLine.taxes!!.first { it.rateId == refundTaxItem.taxRateId }
                    tax.rateId == refundTaxItem.taxRateId &&
                        tax.total.isEqualTo(refundTaxItem.refundTotal)
                }
            }
        }
    }

    @Test
    fun `when preparing refund for fees, then pass correct tax rates`() {
        testBlocking {
            val order = OrderTestUtils.generateOrderWithOneShipping()
            whenever(orderStore.getOrderByIdAndSite(any(), any())).thenReturn(order)

            initViewModel()
            viewModel.onSelectButtonTapped()

            val feeLines = viewModel.viewState.getOrAwaitValue().feesSection.feeRefundLines
            feeLines.map { it.toDataModel() }.forEach { refundItem ->
                val feeLine = order.getFeeLineList().first { it.id == refundItem.itemId }
                assertThat(refundItem.refundTax).allMatch { refundTaxItem ->
                    val tax = feeLine.taxes!!.first { it.rateId == refundTaxItem.taxRateId }
                    tax.rateId == refundTaxItem.taxRateId &&
                        tax.total.isEqualTo(refundTaxItem.refundTotal)
                }
            }
        }
    }
}
