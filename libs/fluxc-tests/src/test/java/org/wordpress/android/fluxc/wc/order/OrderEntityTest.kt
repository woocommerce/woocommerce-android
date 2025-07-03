package org.wordpress.android.fluxc.wc.order

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.Test
import java.util.Collections
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.order.LineItem
import org.wordpress.android.fluxc.model.order.ShippingLine
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
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

    @Test
    fun testGetTaxLinesHandlesInvalidJson() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            taxLines = """[{
            "id": 1,
            "rate_id": "stripe_tax_for_woocommerce__shipping_tax__0__Shipping Tax",
            "code": "TAX",
            "title": "Shipping Tax",
            "total": "5.00",
            "compound": false
        }]"""
        )

        // WHEN
        val taxLines = model.getTaxLineList()

        // THEN
        assertEquals(0, taxLines.size, "Should return empty list when tax lines contains invalid values")
    }

    @Test
    fun testGetTaxLinesValidJson() {
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            taxLines = """[{
            "id": 1,
            "rate_id": 5,
            "rate_code": "TAX-1",
            "rate_percent": 7.5,
            "label": "State Tax",
            "compound": false,
            "tax_total": "5.50",
            "shipping_tax_total": "0.75"
        }, {
            "id": 2,
            "rate_id": 6,
            "rate_code": "TAX-2",
            "rate_percent": 2.0,
            "label": "County Tax",
            "compound": true,
            "tax_total": "2.30",
            "shipping_tax_total": "0.25"
        }]"""
        )

        // WHEN
        val taxLines = model.getTaxLineList()

        // THEN
        assertEquals(2, taxLines.size)

        with(taxLines[0]) {
            assertEquals(1, id)
            assertEquals(5, rateId)
            assertEquals("TAX-1", rateCode)
            assertEquals(7.5f, ratePercent)
            assertEquals("State Tax", label)
            assertEquals(false, compound)
            assertEquals("5.50", taxTotal)
            assertEquals("0.75", shippingTaxTotal)
        }

        with(taxLines[1]) {
            assertEquals(2, id)
            assertEquals(6, rateId)
            assertEquals("TAX-2", rateCode)
            assertEquals(2.0f, ratePercent)
            assertEquals("County Tax", label)
            assertEquals(true, compound)
            assertEquals("2.30", taxTotal)
            assertEquals("0.25", shippingTaxTotal)
        }
    }

    @Test
    fun testParseJsonListSafelyWithValidJson() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = """[
                {"id": 1, "name": "Product 1", "total": "10.00"},
                {"id": 2, "name": "Product 2", "total": "20.00"}
            ]"""
        )

        // WHEN
        val lineItems = model.getLineItemList()

        // THEN
        assertEquals(2, lineItems.size)
        assertEquals("Product 1", lineItems[0].name)
        assertEquals("Product 2", lineItems[1].name)
    }

    @Test
    fun testParseJsonListSafelyWithEmptyString() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = "",
            shippingLines = "",
            feeLines = "",
            couponLines = "",
            taxLines = ""
        )

        // WHEN
        val lineItems = model.getLineItemList()
        val shippingLines = model.getShippingLineList()
        val feeLines = model.getFeeLineList()
        val couponLines = model.getCouponLineList()
        val taxLines = model.getTaxLineList()

        // THEN
        assertTrue(lineItems.isEmpty())
        assertTrue(shippingLines.isEmpty())
        assertTrue(feeLines.isEmpty())
        assertTrue(couponLines.isEmpty())
        assertTrue(taxLines.isEmpty())
    }

    @Test
    fun testParseJsonListSafelyWithMalformedJson() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = "[{invalid json",
            shippingLines = "not json at all",
            feeLines = "{\"key\": \"value\"}",
            taxLines = "[{\"id\": 1,]"
        )

        // WHEN
        val lineItems = model.getLineItemList()
        val shippingLines = model.getShippingLineList()
        val feeLines = model.getFeeLineList()
        val taxLines = model.getTaxLineList()

        // THEN
        assertTrue(lineItems.isEmpty())
        assertTrue(shippingLines.isEmpty())
        assertTrue(feeLines.isEmpty())
        assertTrue(taxLines.isEmpty())
    }

    @Test
    fun testParseJsonListSafelyWithNumberFormatException() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = """[{
                "id": "not_a_number",
                "name": "Product",
                "product_id": "12.5.6",
                "quantity": "abc",
                "total": "10.00"
            }]""",
            taxLines = """[{
                "id": 1,
                "rate_id": "invalid_long",
                "rate_percent": "not_a_float",
                "tax_total": "5.00"
            }]"""
        )

        // WHEN
        val lineItems = model.getLineItemList()
        val taxLines = model.getTaxLineList()

        // THEN
        assertTrue(lineItems.isEmpty())
        assertTrue(taxLines.isEmpty())
    }

    @Test
    fun testParseJsonListSafelyWithNullValues() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = "null",
            shippingLines = "[null, null]",
            feeLines = """[{"id": null, "name": null}]"""
        )

        // WHEN
        val lineItems = model.getLineItemList()
        val shippingLines = model.getShippingLineList()
        val feeLines = model.getFeeLineList()

        // THEN
        assertTrue(lineItems.isEmpty())
        assertEquals(2, shippingLines.size)
        assertEquals(1, feeLines.size)
    }

    @Test
    fun testParseJsonListSafelyWithMixedValidAndInvalidData() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            couponLines = """[
                {"id": 1, "code": "VALID10", "discount": "10.00"},
                {"id": "invalid", "code": "INVALID", "discount": "abc"},
                {"id": 3, "code": "VALID20", "discount": "20.00"}
            ]"""
        )

        // WHEN
        val couponLines = model.getCouponLineList()

        // THEN
        assertTrue(couponLines.isEmpty())
    }

    @Test
    fun testParseJsonListSafelyWithLargeNumbers() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = """[{
                "id": 9223372036854775807,
                "name": "Product with max long id",
                "product_id": 999999999999999999999999999999,
                "total": "10.00"
            }]"""
        )

        // WHEN
        val lineItems = model.getLineItemList()

        // THEN
        assertTrue(lineItems.isEmpty())
    }

    @Test
    fun testParseJsonListSafelyWithSpecialCharacters() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = """[{
                "id": 1,
                "name": "Product with \"quotes\" and \n newlines",
                "sku": "test\\slash",
                "total": "10.00"
            }]"""
        )

        // WHEN
        val lineItems = model.getLineItemList()

        // THEN
        assertEquals(1, lineItems.size)
        assertTrue(lineItems[0].name?.contains("quotes") == true)
    }

    @Test
    fun testParseJsonListSafelyWithEmptyArray() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = "[]",
            shippingLines = "[]",
            feeLines = "[]",
            couponLines = "[]",
            taxLines = "[]"
        )

        // WHEN
        val lineItems = model.getLineItemList()
        val shippingLines = model.getShippingLineList()
        val feeLines = model.getFeeLineList()
        val couponLines = model.getCouponLineList()
        val taxLines = model.getTaxLineList()

        // THEN
        assertTrue(lineItems.isEmpty())
        assertTrue(shippingLines.isEmpty())
        assertTrue(feeLines.isEmpty())
        assertTrue(couponLines.isEmpty())
        assertTrue(taxLines.isEmpty())
    }

    @Test
    fun testCachingBehavior() {
        // GIVEN
        val json = """[
            {"id": 1, "name": "Product 1", "total": "10.00"},
            {"id": 2, "name": "Product 2", "total": "20.00"}
        ]"""
        val model = OrderTestUtils.generateSampleOrder(61).copy(lineItems = json)

        // WHEN
        val startTime1 = System.nanoTime()
        val firstCall = model.getLineItemList()
        val endTime1 = System.nanoTime()
        val firstCallDuration = (endTime1 - startTime1) / 1_000_000.0

        val startTime2 = System.nanoTime()
        val secondCall = model.getLineItemList()
        val endTime2 = System.nanoTime()
        val secondCallDuration = (endTime2 - startTime2) / 1_000_000.0

        val startTime3 = System.nanoTime()
        val thirdCall = model.getLineItemList()
        val endTime3 = System.nanoTime()
        val thirdCallDuration = (endTime3 - startTime3) / 1_000_000.0

        // THEN
        assertEquals(firstCall, secondCall)
        assertEquals(secondCall, thirdCall)
        println("\nCaching Performance Test:")
        println("First call (parse): ${"%.3f".format(firstCallDuration)} ms")
        println("Second call (cached): ${"%.3f".format(secondCallDuration)} ms")
        println("Third call (cached): ${"%.3f".format(thirdCallDuration)} ms")
        println("Speed improvement: ${"%.1f".format(firstCallDuration / secondCallDuration)}x faster")
        
        assertTrue(secondCallDuration < firstCallDuration * 0.1, 
            "Cached call should be at least 10x faster than first call")
    }

    @Test
    fun testCachingWithDifferentOrderIds() {
        // GIVEN
        val json = """[{"id": 1, "name": "Product", "total": "10.00"}]"""
        val model1 = OrderTestUtils.generateSampleOrder(61).copy(lineItems = json)
        val model2 = OrderTestUtils.generateSampleOrder(62).copy(lineItems = json)

        // WHEN
        val list1FirstCall = model1.getLineItemList()
        val list1SecondCall = model1.getLineItemList()
        val list2FirstCall = model2.getLineItemList()
        val list2SecondCall = model2.getLineItemList()

        // THEN
        assertEquals(list1FirstCall, list1SecondCall)
        assertEquals(list2FirstCall, list2SecondCall)
        assertEquals(list1FirstCall.size, list2FirstCall.size)
    }

    @Test
    fun testCachingWithDifferentJsonContent() {
        // GIVEN
        val json1 = """[{"id": 1, "name": "Product 1", "total": "10.00"}]"""
        val json2 = """[{"id": 2, "name": "Product 2", "total": "20.00"}]"""
        val model1 = OrderTestUtils.generateSampleOrder(61).copy(lineItems = json1)
        val model2 = OrderTestUtils.generateSampleOrder(61).copy(lineItems = json2)

        // WHEN
        val list1 = model1.getLineItemList()
        val list2 = model2.getLineItemList()

        // THEN
        assertNotEquals(list1[0].name, list2[0].name)
        assertEquals("Product 1", list1[0].name)
        assertEquals("Product 2", list2[0].name)
    }

    @Test
    fun testCachingAcrossAllListTypes() {
        // GIVEN
        val model = OrderTestUtils.generateSampleOrder(61).copy(
            lineItems = """[{"id": 1, "name": "Product", "total": "10.00"}]""",
            shippingLines = """[{"id": 1, "method_title": "Flat Rate", "total": "5.00"}]""",
            feeLines = """[{"id": 1, "name": "Fee", "total": "2.00"}]""",
            couponLines = """[{"id": 1, "code": "DISCOUNT10", "discount": "10.00"}]""",
            taxLines = """[{"id": 1, "rate_id": 1, "tax_total": "3.00"}]"""
        )

        // WHEN - First calls (should parse)
        val lineItems1 = model.getLineItemList()
        val shippingLines1 = model.getShippingLineList()
        val feeLines1 = model.getFeeLineList()
        val couponLines1 = model.getCouponLineList()
        val taxLines1 = model.getTaxLineList()

        // WHEN - Second calls (should be cached)
        val startTime = System.nanoTime()
        val lineItems2 = model.getLineItemList()
        val shippingLines2 = model.getShippingLineList()
        val feeLines2 = model.getFeeLineList()
        val couponLines2 = model.getCouponLineList()
        val taxLines2 = model.getTaxLineList()
        val endTime = System.nanoTime()
        val totalCachedDuration = (endTime - startTime) / 1_000_000.0

        // THEN
        assertEquals(lineItems1, lineItems2)
        assertEquals(shippingLines1, shippingLines2)
        assertEquals(feeLines1, feeLines2)
        assertEquals(couponLines1, couponLines2)
        assertEquals(taxLines1, taxLines2)
        
        println("\nAll cached calls total time: ${"%.3f".format(totalCachedDuration)} ms")
        assertTrue(totalCachedDuration < 5.0, "All cached calls should complete in under 5ms")
    }

    @Test
    fun testCachingThreadSafety() {
        // GIVEN
        val itemCount = 100
        val json = (1..itemCount).joinToString(",", "[", "]") {
            """{"id": $it, "name": "Product $it", "total": "${it * 10}.00"}"""
        }
        val model = OrderTestUtils.generateSampleOrder(61).copy(lineItems = json)
        val results = Collections.synchronizedList(mutableListOf<List<LineItem>>())
        val threads = mutableListOf<Thread>()

        // WHEN
        repeat(10) {
            val thread = Thread {
                repeat(100) {
                    results.add(model.getLineItemList())
                }
            }
            threads.add(thread)
            thread.start()
        }

        threads.forEach { it.join() }

        // THEN
        assertEquals(1000, results.size)
        results.forEach { list ->
            assertEquals(itemCount, list.size)
            assertEquals("Product 1", list[0].name)
            assertEquals("Product $itemCount", list[itemCount - 1].name)
        }
    }
}
