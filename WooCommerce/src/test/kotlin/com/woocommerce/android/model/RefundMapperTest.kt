package com.woocommerce.android.model

import org.junit.Assert.assertEquals
import org.junit.Test
import org.wordpress.android.fluxc.model.metadata.WCMetaData
import org.wordpress.android.fluxc.model.metadata.WCMetaDataValue
import org.wordpress.android.fluxc.model.refunds.WCRefundModel
import java.math.BigDecimal

class RefundMapperTest {
    @Test
    fun `when mapping refund item from domain model, then extract its properties correctly`() {
        val domainModel = WCRefundModel.WCRefundItem(
            productId = 1,
            quantity = -1,
            itemId = 2,
            name = "name",
            variationId = null,
            subtotal = BigDecimal.TEN,
            total = BigDecimal.TEN,
            totalTax = BigDecimal.TEN,
            sku = "sku",
            price = BigDecimal.TEN,
            metaData = listOf(WCMetaData(id = 0, key = "_refunded_item_id", value = WCMetaDataValue(10)))
        )

        val appModel = domainModel.toAppModel()

        assertEquals(1, appModel.productId)
        assertEquals(1, appModel.quantity)
        assertEquals(2, appModel.id)
        assertEquals("name", appModel.name)
        assertEquals(-1, appModel.variationId)
        assertEquals(BigDecimal.TEN.negate(), appModel.subtotal)
        assertEquals(BigDecimal.TEN.negate(), appModel.total)
        assertEquals(BigDecimal.TEN.negate(), appModel.totalTax)
        assertEquals("sku", appModel.sku)
        assertEquals(BigDecimal.TEN, appModel.price)
        assertEquals(10, appModel.orderItemId)
    }

    @Test
    fun `given item ID is wrapped as String, when mapping refund item from domain model, then extract it correctly`() {
        val domainModel = WCRefundModel.WCRefundItem(
            productId = 1,
            quantity = -1,
            itemId = 2,
            name = "name",
            variationId = null,
            subtotal = BigDecimal.TEN,
            total = BigDecimal.TEN,
            totalTax = BigDecimal.TEN,
            sku = "sku",
            price = BigDecimal.TEN,
            metaData = listOf(WCMetaData(id = 0, key = "_refunded_item_id", value = WCMetaDataValue("10")))
        )

        val appModel = domainModel.toAppModel()

        assertEquals(10, appModel.orderItemId)
    }

    @Test
    fun `when mapping shipping lines from domain model, then extract its properties correctly`() {
        val domainModel = WCRefundModel.WCRefundShippingLine(
            id = 1,
            methodId = "methodId",
            methodTitle = "methodTitle",
            totalTax = BigDecimal.TEN,
            total = BigDecimal.TEN,
            metaData = listOf(WCMetaData(id = 0, key = "_refunded_item_id", value = WCMetaDataValue(10)))
        )

        val appModel = domainModel.toAppModel()

        // Intentionally not checking all properties, we have an inconsistency in the function where
        // we negate the totalTax but not the total, and there is no explanation for this.
        // See https://github.com/woocommerce/woocommerce-android/pull/5517#discussion_r774573314
        // We should update the test when we have a clear understanding of the expected behavior.
        // Note: these properties are not used in the app currently.
        assertEquals(10, appModel.itemId)
    }

    @Test
    fun `when mapping fee lines from domain model, then extract its properties correctly`() {
        val domainModel = WCRefundModel.WCRefundFeeLine(
            id = 1,
            name = "name",
            totalTax = BigDecimal.TEN,
            total = BigDecimal.TEN,
            metaData = listOf(WCMetaData(id = 0, key = "_refunded_item_id", value = WCMetaDataValue(10)))
        )

        val appModel = domainModel.toAppModel()

        // See comment in the previous test.
        assertEquals(10, appModel.id)
    }
}
