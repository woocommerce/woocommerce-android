package com.woocommerce.android.model

import com.woocommerce.android.ui.orders.creation.shipping.toShippingLineDetails
import junit.framework.TestCase.assertEquals
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ShippingLineDetailsTest {
    private val defaultShippingMethods = List(3) { i ->
        ShippingMethod(
            id = "method$i",
            title = "methodTitle$i"
        )
    }

    private val defaultShippingLines = List(3) { i ->
        Order.ShippingLine(
            itemId = i.toLong(),
            methodId = "method$i",
            methodTitle = "shippingLineTitle$i",
            totalTax = BigDecimal.TEN * i.toBigDecimal(),
            total = BigDecimal.ZERO
        )
    }

    @Test
    fun `toShippingLineDetails should return null for empty list`() {
        val shippingLines = emptyList<Order.ShippingLine>()
        val result = shippingLines.toShippingLineDetails(defaultShippingMethods)
        assertNull(result)
    }

    @Test
    fun `toShippingLineDetails should use method title from the shipping method field`() {
        val result = defaultShippingLines.toShippingLineDetails(defaultShippingMethods)
        assertNotNull(result)
        result.forEachIndexed { index, shippingLineDetails ->
            // Use the method title from Order.ShippingLine as name
            assertEquals(shippingLineDetails.name, "shippingLineTitle$index")
            // Use the method title from ShippingMethod as the method title
            assertEquals(shippingLineDetails.shippingMethod!!.title, "methodTitle$index")
        }
    }

    @Test
    fun `toShippingLineDetails should skip shipping lines with null methodId`() {
        val shippingLines = defaultShippingLines.toMutableList()
        shippingLines.add(
            // Because this shipping line methodId is null it should be discarded
            Order.ShippingLine(
                itemId = 33L,
                methodId = null,
                methodTitle = "shippingLineDiscarded",
                totalTax = BigDecimal.TEN,
                total = BigDecimal.ZERO
            )
        )
        val result = shippingLines.toShippingLineDetails(defaultShippingMethods)
        assertNotNull(result)
        assertNotEquals(result.size, shippingLines.size)
        assertEquals(result.size, defaultShippingLines.size)
    }
}
