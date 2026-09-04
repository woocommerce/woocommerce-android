package com.woocommerce.android.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class OrderItemTest {
    private lateinit var orderItemUnderTest: Order.Item

    @Before
    fun setUp() {
        orderItemUnderTest = Order.Item(
            itemId = 123,
            productId = 123,
            name = "test-name",
            price = (123).toBigDecimal(),
            sku = "test-sku",
            quantity = 123f,
            subtotal = (123).toBigDecimal(),
            subtotalTax = (123).toBigDecimal(),
            totalTax = (123).toBigDecimal(),
            total = (123).toBigDecimal(),
            variationId = 123,
            attributesList = listOf()
        )
    }

    @Test
    fun `should parse attributeList to String description correctly`() {
        orderItemUnderTest = orderItemUnderTest.copy(
            attributesList = listOf(
                Order.Item.Attribute("First Key", "First Value"),
                Order.Item.Attribute("Second Key", "Second Value")
            )
        )
        assertEquals("First Value, Second Value", orderItemUnderTest.attributesDescription)
    }

    @Test
    fun `should ignore empty values from the String description`() {
        orderItemUnderTest = orderItemUnderTest.copy(
            attributesList = listOf(
                Order.Item.Attribute("First Key", "First Value"),
                Order.Item.Attribute("Empty Key", ""),
                Order.Item.Attribute("Second Key", "Second Value")
            )
        )
        assertEquals("First Value, Second Value", orderItemUnderTest.attributesDescription)
    }

    @Test
    fun `given attributes with keys, when displayableAttributes is read, then keys are kept`() {
        // GIVEN
        orderItemUnderTest = orderItemUnderTest.copy(
            attributesList = listOf(
                Order.Item.Attribute("Gift message", "<b>write this</b> on the label"),
                Order.Item.Attribute("Color", "red")
            )
        )

        // WHEN
        val attributes = orderItemUnderTest.displayableAttributes

        // THEN
        assertThat(attributes).containsExactly(
            Order.Item.Attribute("Gift message", "Write this on the label"),
            Order.Item.Attribute("Color", "Red")
        )
    }

    @Test
    fun `given attributes without a key or a value, when displayableAttributes is read, then they are dropped`() {
        // GIVEN
        orderItemUnderTest = orderItemUnderTest.copy(
            attributesList = listOf(
                Order.Item.Attribute("First Key", "First Value"),
                Order.Item.Attribute("Empty Value", ""),
                Order.Item.Attribute("", "Orphan Value")
            )
        )

        // WHEN
        val attributes = orderItemUnderTest.displayableAttributes

        // THEN
        assertThat(attributes).containsExactly(Order.Item.Attribute("First Key", "First Value"))
    }
}
