package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.orders.OrderTestUtils
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.settings.CurrencyPosition
import org.wordpress.android.fluxc.model.settings.Settings
import org.wordpress.android.fluxc.store.WooCommerceStore
import java.math.BigDecimal
import kotlin.test.Test

class WooPosGroupRefundItemsTest {
    private val selectedSite: SelectedSite = mock()
    private val wooCommerceStore: WooCommerceStore = mock()
    private lateinit var sut: WooPosGroupRefundItems

    private val testSite = SiteModel().apply { id = 1 }

    @Before
    fun setup() {
        whenever(selectedSite.get()).thenReturn(testSite)
        whenever(wooCommerceStore.getSiteSettings(testSite)).thenReturn(
            Settings(
                currencyCode = "USD",
                currencyPosition = CurrencyPosition.LEFT,
                currencyThousandSeparator = ",",
                currencyDecimalSeparator = ".",
                currencyDecimalNumber = 2,
                countryCode = "US",
                stateCode = "CA",
                address = "",
                address2 = "",
                city = "",
                postalCode = "",
                couponsEnabled = true
            )
        )
        sut = WooPosGroupRefundItems()
    }

    private fun createRefundableItem(
        orderItemId: Long,
        productId: Long = 100L,
        variationId: Long = 0L,
        name: String = "Test Product",
        unitPrice: BigDecimal = BigDecimal("20.00"),
        unitTax: BigDecimal = BigDecimal("2.00"),
        rowIndex: Int = 0
    ) = WooPosRefundableItem(
        orderItemId = orderItemId,
        productId = productId,
        variationId = variationId,
        name = name,
        unitPrice = unitPrice,
        unitTax = unitTax,
        formattedUnitPrice = "$$unitPrice",
        formattedUnitTax = "$$unitTax",
        rowIndex = rowIndex
    )

    private fun createOrderItem(
        itemId: Long,
        quantity: Float = 1f,
        price: BigDecimal = BigDecimal("20.00"),
        totalTax: BigDecimal = BigDecimal.ZERO,
        taxes: List<Order.LineTaxEntry> = emptyList()
    ) = Order.Item(
        itemId = itemId,
        productId = 100L,
        name = "Test Product",
        price = price,
        sku = "",
        quantity = quantity,
        subtotal = price.multiply(quantity.toBigDecimal()),
        subtotalTax = BigDecimal.ZERO,
        totalTax = totalTax,
        total = price.multiply(quantity.toBigDecimal()),
        variationId = 0L,
        attributesList = emptyList(),
        taxes = taxes
    )

    private fun createOrder(items: List<Order.Item>) =
        OrderTestUtils.generateTestOrder().copy(items = items)

    @Test
    fun `given empty list, when invoke called, then returns empty list`() {
        // GIVEN
        val refundableItems = emptyList<WooPosRefundableItem>()
        val order = createOrder(emptyList())

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given single item, when invoke called, then returns single refund item with quantity 1 and correct totals`() {
        // GIVEN
        val orderItem = createOrderItem(
            itemId = 123L,
            quantity = 5f,
            price = BigDecimal("20.00"),
            totalTax = BigDecimal("10.00"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("10.00"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"))
        )
        val order = createOrder(listOf(orderItem))

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(1)
        assertThat(result[0].refundTotal).isEqualTo(BigDecimal("20.00"))
        assertThat(result[0].refundTax).hasSize(1)
        assertThat(result[0].refundTax[0].taxRateId).isEqualTo(1L)
        assertThat(result[0].refundTax[0].refundTotal).isEqualTo(BigDecimal("2.00")) // 10.00 / 5 * 1
    }

    @Test
    fun `given multiple items with same orderItemId, when invoke called, then groups them with correct quantity and totals`() {
        // GIVEN
        val orderItem = createOrderItem(
            itemId = 123L,
            quantity = 5f,
            price = BigDecimal("20.00"),
            totalTax = BigDecimal("10.00"),
            taxes = listOf(
                Order.LineTaxEntry(rateId = 1L, taxAmount = BigDecimal("10.00"))
            )
        )
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 0),
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 1),
            createRefundableItem(orderItemId = 123L, unitPrice = BigDecimal("20.00"), rowIndex = 2)
        )
        val order = createOrder(listOf(orderItem))

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(3)
        assertThat(result[0].refundTotal).isEqualTo(BigDecimal("60.00")) // 20 * 3
        assertThat(result[0].refundTax).hasSize(1)
        assertThat(result[0].refundTax[0].taxRateId).isEqualTo(1L)
        assertThat(result[0].refundTax[0].refundTotal).isEqualTo(BigDecimal("6.00")) // (10.00 / 5) * 3
    }

    @Test
    fun `given items with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L),
            createRefundableItem(orderItemId = 456L),
            createRefundableItem(orderItemId = 789L)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L),
                createOrderItem(itemId = 456L),
                createOrderItem(itemId = 789L)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(3)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L, 789L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given mixed orderItemIds, when invoke called, then groups correctly by orderItemId`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, rowIndex = 1),
            createRefundableItem(orderItemId = 789L, rowIndex = 0),
            createRefundableItem(orderItemId = 456L, rowIndex = 1)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L, quantity = 5f),
                createOrderItem(itemId = 456L, quantity = 5f),
                createOrderItem(itemId = 789L, quantity = 5f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(3)

        val item123 = result.find { it.itemId == 123L }
        assertThat(item123).isNotNull
        assertThat(item123!!.quantity).isEqualTo(2)

        val item456 = result.find { it.itemId == 456L }
        assertThat(item456).isNotNull
        assertThat(item456!!.quantity).isEqualTo(2)

        val item789 = result.find { it.itemId == 789L }
        assertThat(item789).isNotNull
        assertThat(item789!!.quantity).isEqualTo(1)
    }

    @Test
    fun `given 5 items with same orderItemId, when invoke called, then returns single item with quantity 5`() {
        // GIVEN
        val refundableItems = (0..4).map { index ->
            createRefundableItem(orderItemId = 999L, rowIndex = index)
        }
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 999L, quantity = 10f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(999L)
        assertThat(result[0].quantity).isEqualTo(5)
    }

    @Test
    fun `given items with same product and same orderItemId, when invoke called, then groups by orderItemId`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 0),
            createRefundableItem(orderItemId = 123L, productId = 100L, rowIndex = 1)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L, quantity = 2f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(123L)
        assertThat(result[0].quantity).isEqualTo(2)
    }

    @Test
    fun `given same product with different orderItemIds, when invoke called, then creates separate refund items`() {
        // GIVEN
        val refundableItems = listOf(
            createRefundableItem(orderItemId = 123L, productId = 100L),
            createRefundableItem(orderItemId = 456L, productId = 100L)
        )
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 123L),
                createOrderItem(itemId = 456L)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.map { it.itemId }).containsExactlyInAnyOrder(123L, 456L)
        result.forEach { item ->
            assertThat(item.quantity).isEqualTo(1)
        }
    }

    @Test
    fun `given large number of items with same orderItemId, when invoke called, then groups all items correctly`() {
        // GIVEN
        val refundableItems = (0..99).map { index ->
            createRefundableItem(orderItemId = 1L, rowIndex = index)
        }
        val order = createOrder(
            listOf(
                createOrderItem(itemId = 1L, quantity = 100f)
            )
        )

        // WHEN
        val result = sut(refundableItems, order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(1L)
        assertThat(result[0].quantity).isEqualTo(100)
    }

    @Test
    fun `given lump-sum fee row, when invoke called, then emits quantity 0 with full total and taxes`() {
        // GIVEN
        val feeLine = Order.FeeLine(
            id = 777L,
            name = "Service",
            total = BigDecimal("12.50"),
            totalTax = BigDecimal("1.25"),
            taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE,
            taxes = listOf(Order.LineTaxEntry(rateId = 5L, taxAmount = BigDecimal("1.25"))),
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = emptyList(), feesLines = listOf(feeLine))
        val feeRow = WooPosRefundableItem(
            orderItemId = 777L,
            productId = 0L,
            variationId = 0L,
            name = "Service",
            unitPrice = BigDecimal("12.50"),
            unitTax = BigDecimal("1.25"),
            formattedUnitPrice = "$12.50",
            formattedUnitTax = "$1.25",
            rowIndex = 0,
            isLumpSum = true,
        )

        // WHEN
        val result = sut(listOf(feeRow), order, 2)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].itemId).isEqualTo(777L)
        assertThat(result[0].quantity).isEqualTo(0)
        assertThat(result[0].refundTotal).isEqualByComparingTo(BigDecimal("12.50"))
        assertThat(result[0].refundTax).hasSize(1)
        assertThat(result[0].refundTax[0].taxRateId).isEqualTo(5L)
        assertThat(result[0].refundTax[0].refundTotal).isEqualByComparingTo(BigDecimal("1.25"))
    }

    @Test
    fun `given product row and fee row, when invoke called, then both emit with correct quantities`() {
        // GIVEN
        val product = createOrderItem(itemId = 1L, quantity = 1f, price = BigDecimal("10.00"))
        val feeLine = Order.FeeLine(
            id = 99L,
            name = "Tip",
            total = BigDecimal("5.00"),
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE,
            taxes = emptyList(),
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(product), feesLines = listOf(feeLine))
        val rows = listOf(
            createRefundableItem(orderItemId = 1L, unitPrice = BigDecimal("10.00")),
            WooPosRefundableItem(
                orderItemId = 99L,
                productId = 0L,
                variationId = 0L,
                name = "Tip",
                unitPrice = BigDecimal("5.00"),
                unitTax = BigDecimal.ZERO,
                formattedUnitPrice = "$5.00",
                formattedUnitTax = "$0.00",
                rowIndex = 0,
                isLumpSum = true,
            ),
        )

        // WHEN
        val result = sut(rows, order, 2)

        // THEN
        assertThat(result).hasSize(2)
        val productRequest = result.first { it.itemId == 1L }
        val feeRequest = result.first { it.itemId == 99L }
        assertThat(productRequest.quantity).isEqualTo(1)
        assertThat(feeRequest.quantity).isEqualTo(0)
        assertThat(feeRequest.refundTotal).isEqualByComparingTo(BigDecimal("5.00"))
        assertThat(feeRequest.refundTax).isEmpty()
    }
}
