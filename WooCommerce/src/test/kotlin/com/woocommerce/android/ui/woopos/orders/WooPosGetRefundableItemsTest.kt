package com.woocommerce.android.ui.woopos.orders

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
    fun `given order with only non-product items, when invoke called, then returns empty list`() {
        // GIVEN
        val nonProductItem = Order.Item(
            itemId = 1L,
            productId = 0L, // non-product
            name = "Fee Item",
            price = BigDecimal.TEN,
            sku = "",
            quantity = 1f,
            subtotal = BigDecimal.TEN,
            subtotalTax = BigDecimal.ZERO,
            totalTax = BigDecimal.ZERO,
            total = BigDecimal.TEN,
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
        )
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(nonProductItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).isEmpty()
    }

    @Test
    fun `given order with single product item quantity 1 and no refunds, when invoke called, then returns one refundable item with correct details`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 100L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "SKU123",
            quantity = 1f,
            subtotal = BigDecimal("20.00"),
            subtotalTax = BigDecimal("2.00"),
            totalTax = BigDecimal("2.00"),
            total = BigDecimal("20.00"),
            variationId = 0,
            attributesList = emptyList(),
            taxes = emptyList()
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
        assertThat(item.unitTax).isEqualTo(BigDecimal("2.00"))
        assertThat(item.rowIndex).isEqualTo(0)
        assertThat(item.formattedUnitPrice).isEqualTo("USD20.00")
        assertThat(item.formattedUnitTax).isEqualTo("USD2.00")
    }

    @Test
    fun `given order with single product item quantity 3 and no refunds, when invoke called, then returns three refundable items with sequential row indices`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 100L,
            productId = 50L,
            name = "Test Product",
            price = BigDecimal("20.00"),
            sku = "SKU123",
            quantity = 3f,
            subtotal = BigDecimal("60.00"),
            subtotalTax = BigDecimal("6.00"),
            totalTax = BigDecimal("6.00"),
            total = BigDecimal("60.00"),
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
        assertThat(result[0].rowIndex).isEqualTo(0)
        assertThat(result[1].rowIndex).isEqualTo(1)
        assertThat(result[2].rowIndex).isEqualTo(2)
        result.forEach { item ->
            assertThat(item.orderItemId).isEqualTo(100L)
            assertThat(item.productId).isEqualTo(50L)
            assertThat(item.name).isEqualTo("Test Product")
            assertThat(item.unitPrice).isEqualTo(BigDecimal("20.00"))
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.00"))
        }
    }

    @Test
    fun `given order with multiple product items, when invoke called, then returns all items expanded by quantity`() {
        // GIVEN
        val item1 = Order.Item(
            itemId = 1L,
            productId = 10L,
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
            itemId = 2L,
            productId = 20L,
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
        assertThat(result).hasSize(3) // 2 from item1, 1 from item2
        assertThat(result.filter { it.productId == 10L }).hasSize(2)
        assertThat(result.filter { it.productId == 20L }).hasSize(1)
    }

    // Refund Handling

    @Test
    fun `given order with single item and full refund, when invoke called, then returns empty list`() {
        // GIVEN
        val orderItem = Order.Item(
            itemId = 1L,
            productId = 50L,
            name = "Test Product",
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

        val refund = Refund(
            id = 1L,
            amount = BigDecimal("40.00"),
            dateCreated = Date(),
            reason = "Full refund",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 0,
                    quantity = 2,
                    name = "Test Product",
                    subtotal = BigDecimal("40.00"),
                    total = BigDecimal("40.00"),
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
        assertThat(result).isEmpty()
    }

    @Test
    fun `given order with single item and partial refund, when invoke called, then returns remaining quantity as refundable items`() {
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

        val refund = Refund(
            id = 1L,
            amount = BigDecimal("40.00"),
            dateCreated = Date(),
            reason = "Partial refund",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 0,
                    quantity = 2,
                    name = "Test Product",
                    subtotal = BigDecimal("40.00"),
                    total = BigDecimal("40.00"),
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
                    quantity = 2,
                    name = "Product 1",
                    subtotal = BigDecimal("20.00"),
                    total = BigDecimal("20.00"),
                    totalTax = BigDecimal.ZERO,
                    price = BigDecimal("10.00"),
                    orderItemId = 1L
                ),
                Refund.Item(
                    id = 2L,
                    productId = 20L,
                    variationId = 0,
                    quantity = 1,
                    name = "Product 2",
                    subtotal = BigDecimal("15.00"),
                    total = BigDecimal("15.00"),
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
                    quantity = 2,
                    name = "Test Product",
                    subtotal = BigDecimal("40.00"),
                    total = BigDecimal("40.00"),
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
                    quantity = 3,
                    name = "Test Product",
                    subtotal = BigDecimal("60.00"),
                    total = BigDecimal("60.00"),
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
    fun `given order with variation and refund on different variation, when invoke called, then does not reduce quantity`() {
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
            reason = "Refund different variation",
            automaticGatewayRefund = true,
            items = listOf(
                Refund.Item(
                    id = 1L,
                    productId = 50L,
                    variationId = 102L, // Different variation
                    quantity = 1,
                    name = "T-Shirt - Large",
                    subtotal = BigDecimal("20.00"),
                    total = BigDecimal("20.00"),
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
        assertThat(result).hasSize(3) // Quantity unchanged
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
                    quantity = 1,
                    name = "T-Shirt - Small",
                    subtotal = BigDecimal("20.00"),
                    total = BigDecimal("20.00"),
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
    fun `given order item with quantity zero, when invoke called, then unit price equals total`() {
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
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.00"))
        }
    }

    @Test
    fun `given order item with quantity zero, when invoke called, then unit tax equals total tax`() {
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
            assertThat(item.unitTax).isEqualTo(BigDecimal("2.00"))
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
        assertThat(result[0].formattedUnitTax).isEqualTo("GBP2.50")
    }

    // Edge Cases

    @Test
    fun `given order mixing product items and non-product items, when invoke called, then returns only product items`() {
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
        val feeItem = Order.Item(
            itemId = 2L,
            productId = 0L, // Non-product
            name = "Fee",
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
        val order = OrderTestUtils.generateTestOrder().copy(items = listOf(productItem, feeItem))
        val refunds = emptyList<Refund>()

        // WHEN
        val result = sut.invoke(order, refunds)

        // THEN
        assertThat(result).hasSize(1)
        assertThat(result[0].productId).isEqualTo(50L)
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
            assertThat(item.unitPrice).isEqualTo(BigDecimal.ZERO)
            assertThat(item.unitTax).isEqualTo(BigDecimal.ZERO)
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
}
