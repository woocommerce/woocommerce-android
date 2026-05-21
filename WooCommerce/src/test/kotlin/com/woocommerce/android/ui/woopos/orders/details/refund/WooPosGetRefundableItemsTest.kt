package com.woocommerce.android.ui.woopos.orders.details.refund

import com.woocommerce.android.model.Order
import com.woocommerce.android.model.Refund
import com.woocommerce.android.ui.orders.OrderTestUtils
import com.woocommerce.android.util.CurrencyFormatter
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.mockito.kotlin.mock
import java.math.BigDecimal
import java.util.Date
import kotlin.test.Test

class WooPosGetRefundableItemsTest {
    private lateinit var currencyFormatter: CurrencyFormatter
    private lateinit var sut: WooPosGetRefundableItems

    @Before
    fun setup() {
        currencyFormatter = mock(defaultAnswer = { invocation ->
            when (invocation.method.name) {
                "formatCurrency" -> {
                    val amount = invocation.arguments[0] as BigDecimal
                    val currency = invocation.arguments[1] as String
                    "$currency$amount"
                }
                else -> null
            }
        })

        sut = WooPosGetRefundableItems(
            currencyFormatter = currencyFormatter
        )
    }

    private fun generateOrderItem(
        itemId: Long = 1L,
        productId: Long = 50L,
        name: String = "Test Product",
        price: BigDecimal = BigDecimal("20.00"),
        sku: String = "",
        quantity: Float = 1f,
        subtotal: BigDecimal = price.multiply(quantity.toBigDecimal()),
        subtotalTax: BigDecimal = BigDecimal.ZERO,
        totalTax: BigDecimal = BigDecimal.ZERO,
        total: BigDecimal = subtotal,
        variationId: Long = 0,
        attributesList: List<Order.Item.Attribute> = emptyList(),
        taxes: List<Order.LineTaxEntry> = emptyList()
    ) = Order.Item(
        itemId = itemId,
        productId = productId,
        name = name,
        price = price,
        sku = sku,
        quantity = quantity,
        subtotal = subtotal,
        subtotalTax = subtotalTax,
        totalTax = totalTax,
        total = total,
        variationId = variationId,
        attributesList = attributesList,
        taxes = taxes
    )

    private fun generateRefundItem(
        id: Long = 1L,
        productId: Long = 50L,
        variationId: Long = 0,
        quantity: Int = 1,
        name: String = "Test Product",
        price: BigDecimal = BigDecimal("20.00"),
        subtotal: BigDecimal = price.multiply(quantity.toBigDecimal()),
        total: BigDecimal = subtotal,
        totalTax: BigDecimal = BigDecimal.ZERO,
        orderItemId: Long = 1L
    ) = Refund.Item(
        id = id,
        productId = productId,
        variationId = variationId,
        quantity = -quantity,
        name = name,
        subtotal = -subtotal,
        total = -total,
        totalTax = -totalTax,
        price = price,
        orderItemId = orderItemId
    )

    private fun generateRefund(
        id: Long = 1L,
        amount: BigDecimal = BigDecimal("20.00"),
        dateCreated: Date = Date(),
        reason: String = "Test refund",
        automaticGatewayRefund: Boolean = true,
        items: List<Refund.Item> = emptyList(),
        shippingLines: List<Refund.ShippingLine> = emptyList(),
        feeLines: List<Refund.FeeLine> = emptyList()
    ) = Refund(
        id = id,
        amount = amount,
        dateCreated = dateCreated,
        reason = reason,
        automaticGatewayRefund = automaticGatewayRefund,
        items = items,
        shippingLines = shippingLines,
        feeLines = feeLines
    )

    // Basic Cases

    @Test
    fun `given order with no items, when invoke called, then returns empty list`() {
        // GIVEN
        val order = OrderTestUtils.generateTestOrder().copy(items = emptyList())
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given order with deleted product (productId 0), when invoke called, then returns refundable item with product details`() {
        // GIVEN
        val deletedProductItem = generateOrderItem(
            productId = 0L,
            name = "Deleted Product",
            price = BigDecimal("25.00"),
            quantity = 2f,
            totalTax = BigDecimal("5.00")
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(deletedProductItem),
            currency = "USD"
        )
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(2)
        val item = result[0]
        assertThat(item.productId).isEqualTo(0L)
        assertThat(item.name).isEqualTo("Deleted Product")
        assertThat(item.unitPrice).isEqualTo(BigDecimal("25.00"))
        assertThat(item.unitTax).isEqualTo(BigDecimal("2.5"))
        assertThat(item.formattedUnitPrice).isEqualTo("USD25.00")
    }

    @Test
    fun `given order with single product item quantity 1 and no refunds, when invoke called, then returns one refundable item with correct details`() {
        // GIVEN
        val orderItem = generateOrderItem(
            itemId = 100L,
            sku = "SKU123",
            totalTax = BigDecimal("2.00")
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(orderItem),
            currency = "USD"
        )
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        val item = result[0]
        assertThat(item.orderItemId).isEqualTo(100L)
        assertThat(item.productId).isEqualTo(50L)
        assertThat(item.variationId).isEqualTo(0)
        assertThat(item.name).isEqualTo("Test Product")
        assertThat(item.unitPrice).isEqualTo(BigDecimal("20.00"))
        assertThat(item.unitTax).isEqualTo(BigDecimal("2.0"))
        assertThat(item.rowIndex).isEqualTo(0)
        assertThat(item.formattedUnitPrice).isEqualTo("USD20.00")
        assertThat(item.formattedUnitTax).isEqualTo("USD2.0")
    }

    @Test
    fun `given order with single product item quantity 3 and no refunds, when invoke called, then returns three refundable items with sequential row indices`() {
        // GIVEN
        val orderItem = generateOrderItem(
            itemId = 100L,
            quantity = 3f,
            totalTax = BigDecimal("6.00")
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(3)
        assertThat(result[0].rowIndex).isEqualTo(0)
        assertThat(result[1].rowIndex).isEqualTo(1)
        assertThat(result[2].rowIndex).isEqualTo(2)
        result.forEach { item ->
            assertThat(item.orderItemId).isEqualTo(100L)
            assertThat(item.productId).isEqualTo(50L)
            assertThat(item.name).isEqualTo("Test Product")
            assertThat(item.unitPrice).isEqualTo(BigDecimal("20.00"))
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.0"))
        }
    }

    @Test
    fun `given order with multiple product items, when invoke called, then returns all items expanded by quantity`() {
        // GIVEN
        val item1 = generateOrderItem(
            itemId = 1L,
            productId = 10L,
            name = "Product 1",
            price = BigDecimal("10.00"),
            quantity = 2f
        )
        val item2 = generateOrderItem(
            itemId = 2L,
            productId = 20L,
            name = "Product 2",
            price = BigDecimal("15.00")
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(3) // 2 from item1, 1 from item2
        assertThat(result.filter { it.productId == 10L }).hasSize(2)
        assertThat(result.filter { it.productId == 20L }).hasSize(1)
    }

    // Refund Handling

    @Test
    fun `given order with single item and full refund, when invoke called, then returns empty list`() {
        // GIVEN
        val orderItem = generateOrderItem(quantity = 2f)
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refund = generateRefund(
            amount = BigDecimal("40.00"),
            reason = "Full refund",
            items = listOf(generateRefundItem(quantity = 2))
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given order with single item and partial refund, when invoke called, then returns remaining quantity as refundable items`() {
        // GIVEN
        val orderItem = generateOrderItem(quantity = 5f)
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refund = generateRefund(
            amount = BigDecimal("40.00"),
            reason = "Partial refund",
            items = listOf(generateRefundItem(quantity = 2))
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        assertThat(result).hasSize(3) // 5 - 2 = 3 remaining
    }

    @Suppress("LongMethod")
    @Test
    fun `given order with multiple items and partial refunds, when invoke called, then returns correct remaining quantities`() {
        // GIVEN
        val item1 = Order.Item(
            itemId = 1L,
            productId = 10L,
            name = "Product 1",
            price = BigDecimal("10.00"),
            sku = "",
            quantity = 5f,
            subtotal = BigDecimal("50.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("50.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val item2 = Order.Item(
            itemId = 2L,
            productId = 20L,
            name = "Product 2",
            price = BigDecimal("15.00"),
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal("45.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("45.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))

        val refund = Refund(
            id = 1L,
            amount = BigDecimal("40.00"),
            dateCreated = Date(),
            reason = "Partial refund",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 10L,
                    variationId = 0,
                    quantity = -2,
                    name = "Product 1",
                    subtotal = BigDecimal("-20.00"),
                    total = BigDecimal("-20.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("10.00"),
                    orderItemId = 1L
                ),
                Refund.Item(
                    id = 2L,
                    productId = 20L,
                    variationId = 0,
                    quantity = -1,
                    name = "Product 2",
                    subtotal = BigDecimal("-15.00"),
                    total = BigDecimal("-15.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("15.00"),
                    orderItemId = 2L
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        assertThat(result).hasSize(5) // (5-2) + (3-1) = 3 + 2 = 5
        assertThat(result.filter { it.productId == 10L }).hasSize(3)
        assertThat(result.filter { it.productId == 20L }).hasSize(2)
    }

    @Suppress("LongMethod")
    @Test
    fun `given order with multiple refunds for same item, when invoke called, then sums all refunds correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 10f,
            subtotal = BigDecimal("200.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("200.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))

        val refund1 = Refund(
            id = 1L,
            amount = BigDecimal("40.00"),
            dateCreated = Date(),
            reason = "First refund",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 0,
                    quantity = -2,
                    name = "Test Product",
                    subtotal = BigDecimal("-40.00"),
                    total = BigDecimal("-40.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("20.00"),
                    orderItemId = 1L
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        val refund2 = Refund(
            id = 2L,
            amount = BigDecimal("60.00"),
            dateCreated = Date(),
            reason = "Second refund",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 2L,
                    productId = 50L,
                    variationId = 0,
                    quantity = -3,
                    name = "Test Product",
                    subtotal = BigDecimal("-60.00"),
                    total = BigDecimal("-60.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("20.00"),
                    orderItemId = 1L
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund1, refund2))

        // THEN
        assertThat(result).hasSize(5) // 10 - (2 + 3) = 5
    }

    // Variation Matching

    @Test
    fun `given order with same product but different variations, when invoke called, then treats them as separate items`() {
        // GIVEN
        val item1 = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "T-Shirt - Small",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 2f,
            subtotal = BigDecimal("40.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("40.00"),
            variationId = 101L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val item2 = Order.Item(
            itemId = 2L,
            productId = 50L,
            name = "T-Shirt - Large",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal("60.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("60.00"),
            variationId = 102L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(5) // 2 + 3
        assertThat(result.filter { it.variationId == 101L }).hasSize(2)
        assertThat(result.filter { it.variationId == 102L }).hasSize(3)
    }

    @Test
    fun `given order with variation and refund on different order item, when invoke called, then does not reduce quantity`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "T-Shirt - Small",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal("60.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("60.00"),
            variationId = 101L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))

        // Refund targeting a different order item (orderItemId = 999) that doesn't exist in this order
        val refund = Refund(
            id = 1L,
            amount = BigDecimal("20.00"),
            dateCreated = Date(),
            reason = "Refund different order item",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 102L,
                    quantity = -1,
                    name = "T-Shirt - Large",
                    subtotal = BigDecimal("-20.00"),
                    total = BigDecimal("-20.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("20.00"),
                    orderItemId = 999L // Different order item ID
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        assertThat(result).hasSize(3) // Quantity unchanged because refund targets different order item
        result.forEach { item ->
            assertThat(item.variationId).isEqualTo(101L)
        }
    }

    @Test
    fun `given order with variation and refund on same variation, when invoke called, then reduces quantity correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "T-Shirt - Small",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal("60.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("60.00"),
            variationId = 101L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))

        val refund = Refund(
            id = 1L,
            amount = BigDecimal("20.00"),
            dateCreated = Date(),
            reason = "Refund same variation",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 101L, // Same variation
                    quantity = -1,
                    name = "T-Shirt - Small",
                    subtotal = BigDecimal("-20.00"),
                    total = BigDecimal("-20.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("20.00"),
                    orderItemId = 1L
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        assertThat(result).hasSize(2) // 3 - 1 = 2
        result.forEach { item ->
            assertThat(item.variationId).isEqualTo(101L)
        }
    }

    @Suppress("LongMethod")
    @Test
    fun `given order with same product and variation as separate items, when one item refunded, then only that specific item quantity is reduced`() {
        // GIVEN
        // Two separate line items with the same product and variation (e.g., added to cart at different times)
        val item1 = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "T-Shirt - Small",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal("60.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("60.00"),
            variationId = 101L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val item2 = Order.Item(
            itemId = 2L,
            productId = 50L, // Same product
            name = "T-Shirt - Small",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 2f,
            subtotal = BigDecimal("40.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("40.00"),
            variationId = 101L, // Same variation
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))

        // Refund targeting only item1 (orderItemId = 1)
        val refund = Refund(
            id = 1L,
            amount = BigDecimal("40.00"),
            dateCreated = Date(),
            reason = "Partial refund of first item",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 101L,
                    quantity = -2,
                    name = "T-Shirt - Small",
                    subtotal = BigDecimal("-40.00"),
                    total = BigDecimal("-40.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("20.00"),
                    orderItemId = 1L // Refund targets item1 only
                )
            ),
            shippingLines = emptyList(),
            feeLines = emptyList()
        )

        // WHEN
        val result = sut.invoke(order, listOf(refund))

        // THEN
        // Item1: 3 - 2 = 1 remaining
        // Item2: 2 - 0 = 2 remaining (unchanged)
        // Total: 1 + 2 = 3
        assertThat(result).hasSize(3)
        assertThat(result.filter { it.orderItemId == 1L }).hasSize(1)
        assertThat(result.filter { it.orderItemId == 2L }).hasSize(2)
    }

    // Price Calculations

    @Test
    fun `given order item with standard quantity, when invoke called, then calculates unit price correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 4f,
            subtotal = BigDecimal("80.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("80.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(4)
        result.forEach { item ->
            assertThat(item.unitPrice).isEqualTo(BigDecimal("20.00"))
        }
    }

    @Test
    fun `given order item with quantity zero, when invoke called, then returns empty list`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("100.00"),
            sku = "",
            quantity = 0f,
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("100.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).isEmpty() // quantity 0 means maxQuantity will be 0
    }

    @Test
    fun `given order item with decimal quantity, when invoke called, then calculates unit price correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 2.5f,
            subtotal = BigDecimal("50.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("50.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(2) // 2.5 converted to int = 2
        result.forEach { item ->
            assertThat(item.unitPrice).isEqualTo(BigDecimal("20.00"))
        }
    }

    @Test
    fun `given order item with standard quantity, when invoke called, then calculates unit tax correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 5f,
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal("10.00"),
            totalTax = BigDecimal("10.00"),
            total = BigDecimal("100.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(5)
        result.forEach { item ->
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.0"))
        }
    }

    @Test
    fun `given order item with zero quantity and non-zero tax, when invoke called, then returns empty list`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("100.00"),
            sku = "",
            quantity = 0f,
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal("10.00"),
            totalTax = BigDecimal("10.00"),
            total = BigDecimal("100.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).isEmpty() // quantity 0 means maxQuantity will be 0
    }

    @Test
    fun `given order item with decimal quantity, when invoke called, then calculates unit tax correctly`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 3.5f,
            subtotal = BigDecimal("70.00"),
            subtotalTax = BigDecimal("7.00"),
            totalTax = BigDecimal("7.00"),
            total = BigDecimal("70.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(3) // 3.5 converted to int = 3
        result.forEach { item ->
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.0"))
        }
    }

    // Formatting

    @Test
    fun `given order item, when invoke called, then formatted unit price matches currency format`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("25.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("25.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("25.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(orderItem),
            currency = "EUR"
        )
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].formattedUnitPrice).isEqualTo("EUR25.00")
    }

    @Test
    fun `given order item, when invoke called, then formatted unit tax matches currency format`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("25.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("25.00"),
            subtotalTax = BigDecimal("2.50"),
            totalTax = BigDecimal("2.50"),
            total = BigDecimal("25.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(
            items = listOf(orderItem),
            currency = "GBP"
        )
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].formattedUnitTax).isEqualTo("GBP2.5")
    }

    // Edge Cases

    @Test
    fun `given order mixing product items and deleted products, when invoke called, then returns both as refundable items`() {
        // GIVEN
        val productItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("20.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("20.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val deletedProductItem = Order.Item(
            itemId = 2L,
            productId = 0L,
            name = "Deleted Product",
            price = BigDecimal("5.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("5.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("5.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(productItem, deletedProductItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].productId).isEqualTo(50L)
        assertThat(result[0].name).isEqualTo("Product")
        assertThat(result[1].productId).isEqualTo(0L)
        assertThat(result[1].name).isEqualTo("Deleted Product")
    }

    @Test
    fun `given order with zero total but non-zero quantity, when invoke called, then calculates unit price as zero`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Free Product",
            price = BigDecimal.ZERO,
            sku = "",
            quantity = 3f,
            subtotal = BigDecimal.ZERO,
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal.ZERO,
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(3)
        result.forEach { item ->
            assertThat(item.unitPrice).isEqualByComparingTo(BigDecimal.ZERO)
            assertThat(item.unitTax).isEqualByComparingTo(BigDecimal.ZERO)
        }
    }

    @Test
    fun `given order with item ID mapping, when invoke called, then preserves correct order item IDs`() {
        // GIVEN
        val item1 = Order.Item(
            itemId = 123L,
            productId = 50L,
            name = "Product 1",
            price = BigDecimal("10.00"),
            sku = "",
            quantity = 2f,
            subtotal = BigDecimal("20.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("20.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val item2 = Order.Item(
            itemId = 456L,
            productId = 60L,
            name = "Product 2",
            price = BigDecimal("15.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("15.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("15.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(3)
        assertThat(result.filter { it.orderItemId == 123L }).hasSize(2)
        assertThat(result.filter { it.orderItemId == 456L }).hasSize(1)
    }

    // Data Integrity

    @Test
    fun `given order with multiple items, when invoke called, then each refundable item has correct product and variation IDs`() {
        // GIVEN
        val item1 = Order.Item(
            itemId = 1L,
            productId = 100L,
            name = "Product A",
            price = BigDecimal("10.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("10.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("10.00"),
            variationId = 201L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val item2 = Order.Item(
            itemId = 2L,
            productId = 100L,
            name = "Product A - Variant",
            price = BigDecimal("15.00"),
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal("15.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("15.00"),
            variationId = 202L,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(item1, item2))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result[0].productId).isEqualTo(100L)
        assertThat(result[0].variationId).isEqualTo(201L)
        assertThat(result[1].productId).isEqualTo(100L)
        assertThat(result[1].variationId).isEqualTo(202L)
    }

    @Test
    fun `given order with item name, when invoke called, then preserves item name in refundable items`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Special Product Name",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 2f,
            subtotal = BigDecimal("40.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("40.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(2)
        result.forEach { item ->
            assertThat(item.name).isEqualTo("Special Product Name")
        }
    }

    @Test
    fun `given order with quantity 5, when invoke called, then row indices are 0 through 4`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "",
            quantity = 5f,
            subtotal = BigDecimal("100.00"),
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal("100.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(orderItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(5)
        assertThat(result.map { it.rowIndex }).containsExactly(0, 1, 2, 3, 4)
    }

    @Test
    fun `given order with custom amount, when invoke called, then includes lump-sum refundable row`() {
        // GIVEN
        val feeLine = Order.FeeLine(
            id = 42L,
            name = "Service fee",
            total = BigDecimal("12.50"),
            totalTax = BigDecimal("1.25"),
            taxStatus = Order.FeeLine.FeeLineTaxStatus.TAXABLE,
            taxes = emptyList(),
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = emptyList(), feesLines = listOf(feeLine))

        // WHEN
        val result = sut.invoke(order, emptyList())

        // THEN
        assertThat(result).hasSize(1)
        val row = result.first()
        assertThat(row.isLumpSum).isTrue()
        assertThat(row.orderItemId).isEqualTo(42L)
        assertThat(row.name).isEqualTo("Service fee")
        assertThat(row.unitPrice).isEqualByComparingTo(BigDecimal("12.50"))
        assertThat(row.unitTax).isEqualByComparingTo(BigDecimal("1.25"))
        assertThat(row.uniqueId).isEqualTo("fee_42")
    }

    @Test
    fun `given custom amount already refunded, when invoke called, then it is excluded`() {
        // GIVEN
        val feeLine = Order.FeeLine(
            id = 42L,
            name = "Service fee",
            total = BigDecimal("12.50"),
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE,
            taxes = emptyList(),
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = emptyList(), feesLines = listOf(feeLine))
        val previousRefund = Refund(
            id = 1L,
            dateCreated = Date(),
            amount = BigDecimal("12.50"),
            reason = null,
            automaticGatewayRefund = false,
            items = emptyList(),
            shippingLines = emptyList(),
            feeLines = listOf(
                Refund.FeeLine(id = 42L, name = "Service fee", totalTax = BigDecimal.ZERO, total = BigDecimal("-12.50"))
            ),
        )

        // WHEN
        val result = sut.invoke(order, listOf(previousRefund))

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given order with product and custom amount, when invoke called, then both rows are returned`() {
        // GIVEN
        val orderItem = generateOrderItem(itemId = 1L, quantity = 1f, price = BigDecimal("10.00"))
        val feeLine = Order.FeeLine(
            id = 99L,
            name = "Tip",
            total = BigDecimal("5.00"),
            totalTax = BigDecimal.ZERO,
            taxStatus = Order.FeeLine.FeeLineTaxStatus.NONE,
            taxes = emptyList(),
        )
        val order = OrderTestUtils.generateTestOrder()
            .copy(items = listOf(orderItem), feesLines = listOf(feeLine))

        // WHEN
        val result = sut.invoke(order, emptyList())

        // THEN
        assertThat(result).hasSize(2)
        assertThat(result.count { it.isLumpSum }).isEqualTo(1)
        assertThat(result.count { !it.isLumpSum }).isEqualTo(1)
    }
}
