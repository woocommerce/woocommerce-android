@file:Suppress("DEPRECATION_ERROR")
package org.wordpress.android.fluxc.wc.order

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class OrderSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCOrderShipmentTrackingModel::class.java,
                        SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testGetOrderShipmentTrackingsForOrder() {
        val siteModel = SiteModel().apply { id = 1 }
        val orderId = 3L
        val json = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "wc/order-shipment-trackings-multiple.json")
        val trackings = OrderTestUtils
                .getOrderShipmentTrackingsFromJson(json, siteModel.id, orderId)
                .toMutableList()
        assertEquals(2, trackings.size)

        // Save full list to the database
        var rowsAffected = trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
        assertEquals(2, rowsAffected)

        // Attempt to save again (should ignore both existing entries and add new one)
        trackings.add(OrderTestUtils.generateOrderShipmentTracking(siteModel.id, orderId))
        rowsAffected = trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
        assertEquals(1, rowsAffected)

        // Get all shipment trackings for a single order
        val trackingsForOrder = OrderSqlUtils.getShipmentTrackingsForOrder(siteModel, orderId)
        assertEquals(3, trackingsForOrder.size)

        // get a single shipment tracking by tracking number
        val shipmentTracking = OrderSqlUtils.getShipmentTrackingByTrackingNumber(
                siteModel, orderId, trackingsForOrder[0].trackingNumber
        )
        assertNotNull(shipmentTracking)
        assertEquals(trackingsForOrder[0].trackingNumber, shipmentTracking.trackingNumber)
    }

    @Test
    fun testDeleteOrderShipmentTrackingsForSite() {
        val orderId = 3L
        // Insert shipment trackings into the database
        val siteModel = SiteModel().apply { id = 1 }
        val json = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "wc/order-shipment-trackings-multiple.json")
        val trackings = OrderTestUtils
                .getOrderShipmentTrackingsFromJson(json, siteModel.id, orderId)
                .toMutableList()
        assertEquals(2, trackings.size)
        var rowsAffected = trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
        assertEquals(2, rowsAffected)

        // Delete all shipment trackings for site
        rowsAffected = OrderSqlUtils.deleteOrderShipmentTrackingsForSite(siteModel)
        assertEquals(2, rowsAffected)

        // Verify no shipment trackings in db
        val trackingsInDb = OrderSqlUtils.getShipmentTrackingsForOrder(siteModel, orderId)
        assertEquals(0, trackingsInDb.size)
    }

    @Test
    fun testDeleteOrderShipmentTrackingsById() {
        val orderId = 3L
        // Insert shipment trackings into the database
        val siteModel = SiteModel().apply { id = 1 }
        val json = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "wc/order-shipment-trackings-multiple.json")
        val trackings = OrderTestUtils
                .getOrderShipmentTrackingsFromJson(json, siteModel.id, orderId)
                .toMutableList()
        assertEquals(2, trackings.size)
        var rowsAffected = trackings.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentTracking(it) }
        assertEquals(2, rowsAffected)

        // Delete the first shipment tracking
        var trackingsInDb = OrderSqlUtils.getShipmentTrackingsForOrder(siteModel, orderId)
        rowsAffected = OrderSqlUtils.deleteOrderShipmentTrackingById(trackingsInDb[0])
        assertEquals(1, rowsAffected)

        // Verify only a single shipment tracking row in db
        trackingsInDb = OrderSqlUtils.getShipmentTrackingsForOrder(siteModel, orderId)
        assertEquals(1, trackingsInDb.size)
    }
}
