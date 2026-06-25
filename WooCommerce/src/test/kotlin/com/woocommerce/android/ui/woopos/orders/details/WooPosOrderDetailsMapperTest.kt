package com.woocommerce.android.ui.woopos.orders.details

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.ui.woopos.common.data.WooPosGetProductById
import com.woocommerce.android.ui.woopos.orders.OrderStatusColorKey
import com.woocommerce.android.ui.woopos.orders.PosOrderStatus
import com.woocommerce.android.ui.woopos.orders.RefundsFetchResult
import com.woocommerce.android.ui.woopos.orders.WooPosOrderActionsProvider
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.LineItemsState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.RefundsState
import com.woocommerce.android.ui.woopos.orders.WooPosOrdersState.OrderDetailsViewState.Computed.Details.TotalsBreakdown
import com.woocommerce.android.ui.woopos.orders.details.refund.RefundInfo
import com.woocommerce.android.ui.woopos.orders.details.refund.WooPosRefundInfoBuilder
import com.woocommerce.android.ui.woopos.util.WooPosCoroutineTestRule
import com.woocommerce.android.ui.woopos.util.format.WooPosFormatPrice
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.DateTimeUtils
import java.math.BigDecimal
import java.util.Date

@OptIn(ExperimentalCoroutinesApi::class)
class WooPosOrderDetailsMapperTest {
    @Rule
    @JvmField
    val coroutineTestRule = WooPosCoroutineTestRule()

    private val resourceProvider: ResourceProvider = mock()
    private val getProductById: WooPosGetProductById = mock()
    private val formatPrice: WooPosFormatPrice = mock()
    private val orderStatusMapper: WooPosOrderStatusMapper = mock()
    private val refundInfoBuilder: WooPosRefundInfoBuilder = mock()
    private val orderActionsProvider: WooPosOrderActionsProvider = mock()
    private val getNonRefundedItems = WooPosGetNonRefundedItems()
    private val groupRefundedItems = WooPosGroupRefundedItems()

    private val sut = WooPosOrderDetailsMapper(
        resourceProvider = resourceProvider,
        getProductById = getProductById,
        formatPrice = formatPrice,
        orderStatusMapper = orderStatusMapper,
        refundInfoBuilder = refundInfoBuilder,
        orderActionsProvider = orderActionsProvider,
        getNonRefundedItems = getNonRefundedItems,
        groupRefundedItems = groupRefundedItems,
    )

    private val paidOrder: Order = OrderTestUtils.generateTestOrder().copy(
        datePaid = DateTimeUtils.dateUTCFromIso8601("2018-02-02T16:11:13Z"),
        status = Order.Status.Completed,
    )

    @Before
    fun setup() = runBlocking {
        whenever(orderStatusMapper.mapOrderStatus(any())).thenReturn(
            PosOrderStatus(text = "Completed", colorKey = OrderStatusColorKey.COMPLETED)
        )
        whenever(resourceProvider.getString(any())).thenReturn("at")
        whenever(refundInfoBuilder.buildRefundInfo(any(), any())).thenReturn(
            RefundInfo(refundRows = emptyList(), totalRefunded = BigDecimal.ZERO)
        )
        whenever(refundInfoBuilder.buildTotalsBreakdown(any(), any())).thenReturn(
            TotalsBreakdown(
                products = "$10.00",
                discount = null,
                discountCode = null,
                taxes = "$0.00",
                shipping = null,
                refundsState = RefundsState.Loaded(refunds = emptyList()),
                netPayment = null,
            )
        )
        whenever(orderActionsProvider.getAvailableActions(any())).thenReturn(emptyList())
        whenever(formatPrice(any<BigDecimal>(), anyOrNull<String>())).thenReturn("$0.00")
        whenever(formatPrice(any<BigDecimal>())).thenReturn("$0.00")
        Unit
    }

    private suspend fun setupDefaults() {
        whenever(formatPrice(any<BigDecimal>(), any())).thenAnswer { invocation ->
            val amount = invocation.arguments[0] as? BigDecimal
            amount?.let { "$${it.setScale(2)}" } ?: "$0.00"
        }
        whenever(formatPrice(any<BigDecimal>())).thenAnswer { invocation ->
            val amount = invocation.arguments[0] as? BigDecimal
            amount?.let { "$${it.setScale(2)}" } ?: "$0.00"
        }
    }

    private fun createOrderItem(
        itemId: Long,
        productId: Long = 10L,
        name: String = "Product $itemId",
        price: BigDecimal = BigDecimal("10.00"),
        quantity: Float = 1f,
    ) = Order.Item(
        itemId = itemId,
        productId = productId,
        name = name,
        price = price,
        sku = "",
        quantity = quantity,
        subtotal = price * quantity.toBigDecimal(),
        subtotalTax = BigDecimal.ZERO,
        totalTax = BigDecimal.ZERO,
        total = price * quantity.toBigDecimal(),
        variationId = 0,
        attributesList = emptyList(),
    )

    private fun createRefundItem(
        orderItemId: Long,
        productId: Long = 10L,
        quantity: Int = 1,
        total: BigDecimal = BigDecimal("10.00"),
        totalTax: BigDecimal = BigDecimal.ZERO,
        name: String = "Refund Product",
    ) = Refund.Item(
        productId = productId,
        quantity = quantity,
        orderItemId = orderItemId,
        name = name,
        total = total,
        totalTax = totalTax,
        price = if (quantity > 0) total / quantity.toBigDecimal() else total,
    )

    private fun createRefund(
        id: Long = 1L,
        items: List<Refund.Item>,
        amount: BigDecimal = items.fold(BigDecimal.ZERO) { acc, item -> acc + item.total },
    ) = Refund(
        id = id,
        dateCreated = Date(),
        amount = amount,
        reason = null,
        automaticGatewayRefund = false,
        items = items,
        shippingLines = emptyList(),
        feeLines = emptyList(),
    )

    private fun createOrder(items: List<Order.Item>) =
        OrderTestUtils.generateTestOrder().copy(items = items)

    @Test
    fun `given order is paid, when mapOrderDetails, then totalPaid equals formatted total`() = runTest {
        whenever(formatPrice(paidOrder.total, paidOrder.currency)).thenReturn("$106.00")

        val result = sut.mapOrderDetails(paidOrder, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$106.00")
    }

    @Test
    fun `given order is unpaid, when mapOrderDetails, then totalPaid equals formatted zero`() = runTest {
        val unpaidOrder = paidOrder.copy(datePaid = null)
        whenever(formatPrice(unpaidOrder.total, unpaidOrder.currency)).thenReturn("$106.00")
        whenever(formatPrice(BigDecimal.ZERO, unpaidOrder.currency)).thenReturn("$0.00")

        val result = sut.mapOrderDetails(unpaidOrder, RefundsFetchResult.Success(emptyList()))

        assertThat(result.totalPaid).isEqualTo("$0.00")
    }

    @Test
    fun `given order is paid, when mapOrderDetailsWithoutRefunds, then totalPaid equals formatted total`() =
        runTest {
            whenever(formatPrice(paidOrder.total)).thenReturn("$106.00")

            val result = sut.mapOrderDetailsWithoutRefunds(paidOrder)

            assertThat(result.totalPaid).isEqualTo("$106.00")
        }

    @Test
    fun `given order is unpaid, when mapOrderDetailsWithoutRefunds, then totalPaid equals formatted zero`() =
        runTest {
            val unpaidOrder = paidOrder.copy(datePaid = null)
            whenever(formatPrice(unpaidOrder.total)).thenReturn("$106.00")
            whenever(formatPrice(BigDecimal.ZERO)).thenReturn("$0.00")

            val result = sut.mapOrderDetailsWithoutRefunds(unpaidOrder)

            assertThat(result.totalPaid).isEqualTo("$0.00")
        }

    @Test
    fun `given refunds with items, when mapOrderDetails, then refundedLineItems are populated`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 2f),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00"))
                )
            )
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        val refundedItems = (result.refundedLineItems as LineItemsState.Loaded).items
        assertThat(refundedItems).hasSize(1)
        val refundedItem = refundedItems.first()
        assertThat(refundedItem.name).isEqualTo("Cup")
        assertThat(refundedItem.qtyAndUnitPrice).isEqualTo("1 x $4.00")
        assertThat(refundedItem.lineTotal).isEqualTo("$4.00")

        val lineItems = (result.lineItems as LineItemsState.Loaded).items
        assertThat(lineItems).hasSize(1)
        assertThat(lineItems.first().name).isEqualTo("Cup")
        assertThat(lineItems.first().qtyAndUnitPrice).isEqualTo("1 x $4.00")
    }

    @Test
    fun `given multiple refunds for same item, when mapOrderDetails, then quantities are aggregated`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 3f),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(
                id = 1L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00"))
                )
            ),
            createRefund(
                id = 2L,
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 2, total = BigDecimal("8.00"))
                )
            )
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        val refundedItems = (result.refundedLineItems as LineItemsState.Loaded).items
        assertThat(refundedItems).hasSize(1)
        val refundedItem = refundedItems.first()
        assertThat(refundedItem.qtyAndUnitPrice).isEqualTo("3 x $4.00")
        assertThat(refundedItem.lineTotal).isEqualTo("$12.00")
    }

    @Test
    fun `given refunds with no items, when mapOrderDetails, then refundedLineItems is empty`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(id = 1L, items = emptyList(), amount = BigDecimal("4.00"))
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        assertThat((result.refundedLineItems as LineItemsState.Loaded).items).isEmpty()
    }

    @Test
    fun `given refund fetch error, when mapOrderDetails, then refundedLineItems is empty`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
        )
        val order = createOrder(orderItems)
        val refundResult = RefundsFetchResult.Error

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        assertThat((result.refundedLineItems as LineItemsState.Loaded).items).isEmpty()
    }

    @Test
    fun `given order without refunds, when mapOrderDetailsWithoutRefunds, then lineItems loaded and refunds empty`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems)

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            val lineItems = (result.lineItems as LineItemsState.Loaded).items
            assertThat(lineItems).hasSize(1)
            assertThat((result.refundedLineItems as LineItemsState.Loaded).items).isEmpty()
        }

    @Test
    fun `given order with partial refund, when mapOrderDetailsWithoutRefunds, then both lineItems are loading`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems).copy(refundTotal = BigDecimal("2.00"))

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            assertThat(result.lineItems).isInstanceOf(LineItemsState.Loading::class.java)
            assertThat(result.refundedLineItems).isInstanceOf(LineItemsState.Loading::class.java)
        }

    @Test
    fun `given fully refunded order, when mapOrderDetailsWithoutRefunds, then lineItems loaded empty and refundedLineItems loading`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems).copy(
                status = Order.Status.Refunded,
                refundTotal = BigDecimal("4.00")
            )

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            assertThat((result.lineItems as LineItemsState.Loaded).items).isEmpty()
            assertThat(result.refundedLineItems).isInstanceOf(LineItemsState.Loading::class.java)
        }

    @Test
    fun `given refunds for multiple items, when mapOrderDetails, then each item has separate entry`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 2f),
            createOrderItem(itemId = 2L, productId = 20L, name = "Plate", price = BigDecimal("6.00"), quantity = 1f),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00")),
                    createRefundItem(orderItemId = 2L, productId = 20L, quantity = 1, total = BigDecimal("6.00")),
                )
            )
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        val refundedItems = (result.refundedLineItems as LineItemsState.Loaded).items
        assertThat(refundedItems).hasSize(2)
        assertThat(refundedItems.map { it.name }).containsExactly("Cup", "Plate")
        assertThat(refundedItems.map { it.lineTotal }).containsExactly("$4.00", "$6.00")

        val lineItems = (result.lineItems as LineItemsState.Loaded).items
        assertThat(lineItems).hasSize(1)
        assertThat(lineItems.first().name).isEqualTo("Cup")
        assertThat(lineItems.first().qtyAndUnitPrice).isEqualTo("1 x $4.00")
    }

    @Test
    fun `given fully refunded item, when mapOrderDetails, then item is excluded from lineItems`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 1f),
            createOrderItem(itemId = 2L, productId = 20L, name = "Plate", price = BigDecimal("6.00"), quantity = 1f),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00")),
                )
            )
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        val lineItems = (result.lineItems as LineItemsState.Loaded).items
        assertThat(lineItems).hasSize(1)
        assertThat(lineItems.first().name).isEqualTo("Plate")
        val refundedItems = (result.refundedLineItems as LineItemsState.Loaded).items
        assertThat(refundedItems).hasSize(1)
        assertThat(refundedItems.first().name).isEqualTo("Cup")
    }

    @Test
    fun `given no refunds, when mapOrderDetails, then all items shown in lineItems`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 2f),
        )
        val order = createOrder(orderItems)
        val refundResult = RefundsFetchResult.Success(emptyList())

        // WHEN
        val result = sut.mapOrderDetails(order, refundResult)

        // THEN
        val lineItems = (result.lineItems as LineItemsState.Loaded).items
        assertThat(lineItems).hasSize(1)
        assertThat(lineItems.first().name).isEqualTo("Cup")
        assertThat(lineItems.first().qtyAndUnitPrice).isEqualTo("2 x $4.00")
        assertThat((result.refundedLineItems as LineItemsState.Loaded).items).isEmpty()
    }

    @Test
    fun `given refunded item not found in order, when buildRefundedLineItems, then refund item name is used as fallback`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 1f),
            )
            val order = createOrder(orderItems)
            val refunds = listOf(
                createRefund(
                    items = listOf(
                        createRefundItem(
                            orderItemId = 999L,
                            productId = 99L,
                            quantity = 1,
                            total = BigDecimal("5.00"),
                            name = "Deleted Product"
                        )
                    )
                )
            )
            val refundResult = RefundsFetchResult.Success(refunds)

            // WHEN
            val result = sut.buildRefundedLineItems(order, refundResult)

            // THEN
            assertThat(result).hasSize(1)
            assertThat(result.first().name).isEqualTo("Deleted Product")
        }

    @Test
    fun `given same product in different line items, when one is refunded, then only that line item is affected`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(
                    itemId = 1L,
                    productId = 10L,
                    name = "Cup (Red)",
                    price = BigDecimal("4.00"),
                    quantity = 1f
                ),
                createOrderItem(
                    itemId = 2L,
                    productId = 10L,
                    name = "Cup (Blue)",
                    price = BigDecimal("4.00"),
                    quantity = 1f
                ),
            )
            val order = createOrder(orderItems)
            val refunds = listOf(
                createRefund(
                    items = listOf(
                        createRefundItem(orderItemId = 1L, productId = 10L, quantity = 1, total = BigDecimal("4.00"))
                    )
                )
            )
            val refundResult = RefundsFetchResult.Success(refunds)

            // WHEN
            val result = sut.mapOrderDetails(order, refundResult)

            // THEN
            val lineItems = (result.lineItems as LineItemsState.Loaded).items
            assertThat(lineItems).hasSize(1)
            assertThat(lineItems.first().name).isEqualTo("Cup (Blue)")

            val refundedItems = (result.refundedLineItems as LineItemsState.Loaded).items
            assertThat(refundedItems).hasSize(1)
            assertThat(refundedItems.first().name).isEqualTo("Cup (Red)")
        }

    @Test
    fun `given refund with tax, when buildRefundedLineItems, then prices exclude tax`() = runTest {
        // GIVEN
        setupDefaults()
        val orderItems = listOf(
            createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00"), quantity = 2f),
        )
        val order = createOrder(orderItems)
        val refunds = listOf(
            createRefund(
                items = listOf(
                    createRefundItem(
                        orderItemId = 1L,
                        productId = 10L,
                        quantity = 1,
                        total = BigDecimal("4.00"),
                        totalTax = BigDecimal("0.40"),
                    )
                )
            )
        )
        val refundResult = RefundsFetchResult.Success(refunds)

        // WHEN
        val result = sut.buildRefundedLineItems(order, refundResult)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result.first().qtyAndUnitPrice).isEqualTo("1 x $4.00")
        assertThat(result.first().lineTotal).isEqualTo("$4.00")
    }

    @Test
    fun `given refund with negative quantity, when buildRefundedLineItems, then quantity is shown as positive`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(
                    itemId = 1L,
                    productId = 10L,
                    name = "Cup",
                    price = BigDecimal("4.00"),
                    quantity = 2f
                ),
            )
            val order = createOrder(orderItems)
            val refunds = listOf(
                createRefund(
                    items = listOf(
                        createRefundItem(
                            orderItemId = 1L,
                            productId = 10L,
                            quantity = -1,
                            total = BigDecimal("-4.00"),
                            totalTax = BigDecimal("-0.40"),
                        )
                    )
                )
            )
            val refundResult = RefundsFetchResult.Success(refunds)

            // WHEN
            val result = sut.buildRefundedLineItems(order, refundResult)

            // THEN
            assertThat(result).hasSize(1)
            assertThat(result.first().qtyAndUnitPrice).isEqualTo("1 x $4.00")
            assertThat(result.first().lineTotal).isEqualTo("$4.00")
        }

    @Test
    fun `given order with partial refund, when mapOrderDetailsWithoutRefunds, then refundsState is Loading`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems).copy(refundTotal = BigDecimal("2.00"))

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            assertThat(result.breakdown.refundsState).isEqualTo(RefundsState.Loading)
        }

    @Test
    fun `given fully refunded order, when mapOrderDetailsWithoutRefunds, then refundsState is Loading`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems).copy(
                status = Order.Status.Refunded,
                refundTotal = BigDecimal("4.00")
            )

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            assertThat(result.breakdown.refundsState).isEqualTo(RefundsState.Loading)
        }

    @Test
    fun `given order without refunds, when mapOrderDetailsWithoutRefunds, then refundsState is Loaded empty`() =
        runTest {
            // GIVEN
            setupDefaults()
            val orderItems = listOf(
                createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
            )
            val order = createOrder(orderItems)

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            assertThat(result.breakdown.refundsState).isEqualTo(RefundsState.Loaded(refunds = emptyList()))
        }

    @Test
    fun `given order with fee line, when mapOrderDetails, then custom amount row is appended after products`() =
        runTest {
            // GIVEN
            setupDefaults()
            val order = createOrder(
                items = listOf(
                    createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
                )
            ).copy(
                feesLines = listOf(createFeeLine(id = 99L, name = "Gift wrap", total = BigDecimal("2.50")))
            )

            // WHEN
            val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

            // THEN
            val lineItems = (result.lineItems as LineItemsState.Loaded).items
            assertThat(lineItems).hasSize(2)
            assertThat(lineItems[0].isLumpSum).isFalse()
            assertThat(lineItems[1].isLumpSum).isTrue()
            assertThat(lineItems[1].name).isEqualTo("Gift wrap")
            assertThat(lineItems[1].lineTotal).isEqualTo("$2.50")
        }

    @Test
    fun `given taxable fee line, when mapOrderDetails, then row includesTax is true`() = runTest {
        // GIVEN
        setupDefaults()
        val order = createOrder(items = emptyList()).copy(
            feesLines = listOf(
                createFeeLine(
                    id = 1L,
                    name = "Service charge",
                    total = BigDecimal("5.00"),
                    taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE,
                )
            )
        )

        // WHEN
        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

        // THEN
        val row = (result.lineItems as LineItemsState.Loaded).items.single()
        assertThat(row.isLumpSum).isTrue()
        assertThat(row.includesTax).isTrue()
    }

    @Test
    fun `given non-taxable fee line, when mapOrderDetails, then row includesTax is false`() = runTest {
        // GIVEN
        setupDefaults()
        val order = createOrder(items = emptyList()).copy(
            feesLines = listOf(
                createFeeLine(
                    id = 1L,
                    name = "Tip",
                    total = BigDecimal("1.00"),
                    taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE,
                )
            )
        )

        // WHEN
        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

        // THEN
        assertThat((result.lineItems as LineItemsState.Loaded).items.single().includesTax).isFalse()
    }

    @Test
    fun `given unknown tax status fee line, when mapOrderDetails, then row includesTax is false`() = runTest {
        // GIVEN
        setupDefaults()
        val order = createOrder(items = emptyList()).copy(
            feesLines = listOf(
                createFeeLine(
                    id = 1L,
                    name = "Service charge",
                    total = BigDecimal("3.50"),
                    taxStatus = Order.FeeLine.FeeLineTaxStatus.UNKNOWN,
                )
            )
        )

        // WHEN
        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(emptyList()))

        // THEN
        assertThat((result.lineItems as LineItemsState.Loaded).items.single().includesTax).isFalse()
    }

    @Test
    fun `given fee line refunded by id, when mapOrderDetails, then fee row is excluded`() = runTest {
        // GIVEN
        setupDefaults()
        val order = createOrder(items = emptyList()).copy(
            feesLines = listOf(
                createFeeLine(id = 1L, name = "Gift wrap", total = BigDecimal("2.50")),
                createFeeLine(id = 2L, name = "Tip", total = BigDecimal("1.00")),
            )
        )
        val refund = Refund(
            id = 1L,
            dateCreated = Date(),
            amount = BigDecimal("2.50"),
            reason = null,
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLines = emptyList(),
            feeLines = listOf(
                Refund.FeeLine(id = 1L, name = "Gift wrap", totalTax = BigDecimal.ZERO, total = BigDecimal("-2.50"))
            ),
        )

        // WHEN
        val result = sut.mapOrderDetails(order, RefundsFetchResult.Success(listOf(refund)))

        // THEN
        val rows = (result.lineItems as LineItemsState.Loaded).items
        assertThat(rows).hasSize(1)
        assertThat(rows.single().name).isEqualTo("Tip")
    }

    @Test
    fun `given order with fee line, when mapOrderDetailsWithoutRefunds, then custom amount row is appended`() =
        runTest {
            // GIVEN
            setupDefaults()
            val order = createOrder(
                items = listOf(
                    createOrderItem(itemId = 1L, productId = 10L, name = "Cup", price = BigDecimal("4.00")),
                )
            ).copy(
                feesLines = listOf(
                    createFeeLine(
                        id = 99L,
                        name = "Service charge",
                        total = BigDecimal("3.00"),
                        taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE,
                    )
                )
            )

            // WHEN
            val result = sut.mapOrderDetailsWithoutRefunds(order)

            // THEN
            val rows = (result.lineItems as LineItemsState.Loaded).items
            assertThat(rows).hasSize(2)
            assertThat(rows.last().isLumpSum).isTrue()
            assertThat(rows.last().includesTax).isTrue()
            assertThat(rows.last().name).isEqualTo("Service charge")
        }

    private fun createFeeLine(
        id: Long,
        name: String,
        total: BigDecimal,
        totalTax: BigDecimal = BigDecimal.ZERO,
        taxStatus: Order.FeeLine.FeeLineTaxStatus = Order.FeeLine.FeeLineTaxStatus.NONE,
    ) = Order.FeeLine(
        id = id,
        name = name,
        total = total,
        totalTax = totalTax,
        taxStatus = taxStatus,
        taxes = emptyList(),
    )
}
