package org.wordpress.android.fluxc.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.order.ShippingLine
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class OrderEntityTest {
    private val gson = Gson()

    @Test
    fun testGetShippingAddress() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
                shippingAddress1 = "Some place"
        )

        assertEquals("Some place", model.getShippingAddress().address1)
        assertEquals("something else", model.copy(shippingAddress1 = "something else").getShippingAddress().address1)
    }

    @Test
    fun testGetShippingVSBillingAddress() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
                billingAddress1 = "Some place",
                billingCountry = "Canada",
                shippingAddress1 = "A different place",
                shippingCountry = "Canada"
        )

        assertTrue(model.hasSeparateShippingDetails())
        assertEquals("Some place", model.getBillingAddress().address1)
        assertEquals("A different place", model.getShippingAddress().address1)
        assertEquals("something else", model.copy(billingAddress1 = "something else").getBillingAddress().address1)
    }

    @Test
    fun testGetLineItems() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
                lineItems = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/lineitems.json")
        )
        val renderedLineItems = model.getLineItemList()
        assertEquals(3, renderedLineItems.size)

        with(renderedLineItems[0]) {
            assertEquals("A test", name)
            assertEquals(15, productId)
            assertNull(variationId)
            assertEquals("10.00", total)
            assertNull(sku)
            assertNull(parentName)
        }

        with(renderedLineItems[1]) {
            assertEquals("A second test", name)
            assertEquals(65, productId)
            assertEquals(3, variationId)
            assertEquals("20.00", total)
            assertEquals("blabla", sku)
            assertNull(parentName)
        }

        with(renderedLineItems[2]) {
            assertEquals("V-Neck T-Shirt - Blue, Medium", name)
            assertEquals("V-Neck T-Shirt", parentName)
            assertEquals(12, productId)
            assertEquals(8947, variationId)
        }
    }

    @Test
    fun testGetLineItemAttributes() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
                lineItems = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/lineitems.json")
        )
        val renderedLineItems = model.getLineItemList()
        assertEquals(3, renderedLineItems.size)

        with(renderedLineItems[0]) {
            val attributes = getAttributeList()
            assertEquals(2, attributes.size)

            assertEquals("color", attributes[0].key)
            assertEquals("Red", attributes[0].value)

            assertEquals("size", attributes[1].key)
            assertEquals("Medium", attributes[1].value)
        }

        with(renderedLineItems[1]) {
            val attributes = getAttributeList()
            assertEquals(1, attributes.size)

            assertEquals("size", attributes[0].key)
            assertEquals("medium", attributes[0].value)
        }
    }

    @Test
    fun testGetSubtotal() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
                lineItems = "[{\"subtotal\": \"12.26\"},{\"subtotal\": \"15.39\"}]"
        )

        assertEquals(27.65, model.getOrderSubtotal())
        assertEquals(0.0, model.copy(lineItems = "[{\"total\": \"12.26\"},{\"total\": \"15.39\"}]").getOrderSubtotal())
    }

    @Test
    fun testGetShippingLines() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            shippingLines = UnitTestUtils.getStringFromResourceFile(
                    this.javaClass, "wc/order-shipping-lines.json"
            )
        )

        val listShippingLineType = object : TypeToken<List<ShippingLine>>() {}.type
        val shippingLinesList: List<ShippingLine> = gson.fromJson(model.shippingLines, listShippingLineType)

        assertEquals(2, shippingLinesList.count())
    }

    @Test
    fun testGetShippingLinesAttributes() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            shippingLines = UnitTestUtils.getStringFromResourceFile(
                    this.javaClass, "wc/order-shipping-lines.json"
            )
        )

        val listShippingLineType = object : TypeToken<List<ShippingLine>>() {}.type
        val shippingLinesList: List<ShippingLine> = gson.fromJson(model.shippingLines, listShippingLineType)

        assertEquals(2, shippingLinesList[0].id)
        assertEquals(3, shippingLinesList[1].id)

        assertEquals("10.00", shippingLinesList[0].total)
        assertEquals("20.00", shippingLinesList[1].total)

        assertEquals("Flat Rate Shipping", shippingLinesList[0].methodTitle)
        assertEquals("Local Pickup Shipping", shippingLinesList[1].methodTitle)
    }
}
