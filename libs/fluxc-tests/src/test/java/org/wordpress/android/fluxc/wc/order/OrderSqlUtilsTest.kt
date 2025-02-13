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
import org.wordpress.android.fluxc.TestSiteSqlUtils
import org.wordpress.android.fluxc.UnitTestUtils
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderShipmentProviderModel
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.persistence.OrderSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class OrderSqlUtilsTest {
    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        WCOrderStatusModel::class.java,
                        WCOrderShipmentTrackingModel::class.java,
                        WCOrderShipmentProviderModel::class.java,
                        WCOrderSummaryModel::class.java,
                        SiteModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testInsertOrUpdateOrderStatusOptions() {
        val siteModel = SiteModel().apply { id = 1 }
        val optionsJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_status_options.json")
        val orderStatusOptions = OrderTestUtils.getOrderStatusOptionsFromJson(optionsJson, siteModel.id)
        assertEquals(8, orderStatusOptions.size)

        // Save first option to the database, retrieve and verify fields
        val firstOption = orderStatusOptions[0]
        var rowsAffected = OrderSqlUtils.insertOrUpdateOrderStatusOption(firstOption)
        assertEquals(1, rowsAffected)
        val firstOptionDb = OrderSqlUtils.getOrderStatusOptionsForSite(siteModel).first()
        assertNotNull(firstOptionDb)
        assertEquals(firstOptionDb.localSiteId, firstOption.id)
        assertEquals(firstOptionDb.statusKey, firstOption.statusKey)
        assertEquals(firstOptionDb.label, firstOption.label)
        assertEquals(firstOptionDb.statusCount, firstOption.statusCount)

        // Save full list, but only all but the first 2 should be inserted
        rowsAffected = orderStatusOptions.sumBy { OrderSqlUtils.insertOrUpdateOrderStatusOption(it) }
        assertEquals(8, rowsAffected)

        // Update a single option
        val newLabel = "New Label"
        firstOption.apply { label = newLabel }
        rowsAffected = OrderSqlUtils.insertOrUpdateOrderStatusOption(firstOption)
        assertEquals(1, rowsAffected)
        val newFirstOption = OrderSqlUtils.getOrderStatusOptionsForSite(siteModel).first {
            it.statusKey == firstOption.statusKey
        }
        assertEquals(firstOption.label, newFirstOption.label)
        assertEquals(firstOption.statusCount, newFirstOption.statusCount)

        // Get order status options from the database
        val orderStatusOptionsDb = OrderSqlUtils.getOrderStatusOptionsForSite(siteModel)
        assertEquals(8, orderStatusOptionsDb.size)
    }

    @Test
    fun testDeleteOrderStatusOption() {
        val siteModel = SiteModel().apply { id = 1 }
        val optionsJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_status_options.json")
        val orderStatusOptions = OrderTestUtils.getOrderStatusOptionsFromJson(optionsJson, siteModel.id)
        assertEquals(8, orderStatusOptions.size)

        // Save the full list to the database
        var rowsAffected = orderStatusOptions.sumBy { OrderSqlUtils.insertOrUpdateOrderStatusOption(it) }
        assertEquals(8, rowsAffected)

        // Delete the first option from the database
        val firstOption = orderStatusOptions[0]
        rowsAffected = OrderSqlUtils.deleteOrderStatusOption(firstOption)
        assertEquals(1, rowsAffected)

        // Fetch list and verify first option is not present
        val allOptions = OrderSqlUtils.getOrderStatusOptionsForSite(siteModel)
        assertEquals(7, allOptions.size)
        assertFalse { allOptions.contains(firstOption) }
    }

    @Test
    fun testGetOrderStatusOption() {
        val siteModel = SiteModel().apply { id = 1 }
        val optionsJson = UnitTestUtils.getStringFromResourceFile(this.javaClass, "wc/order_status_options.json")
        val orderStatusOptions = OrderTestUtils.getOrderStatusOptionsFromJson(optionsJson, siteModel.id)
        assertEquals(8, orderStatusOptions.size)

        // Save the full list to the database
        val rowsAffected = orderStatusOptions.sumBy { OrderSqlUtils.insertOrUpdateOrderStatusOption(it) }
        assertEquals(8, rowsAffected)

        // Get the first option from the database by the status key
        val firstOption = OrderSqlUtils.getOrderStatusOptionForSiteByKey(siteModel, orderStatusOptions[0].statusKey)
        assertNotNull(firstOption)
        assertEquals(firstOption.label, orderStatusOptions[0].label)
        assertEquals(firstOption.statusCount, orderStatusOptions[0].statusCount)
    }

    @Test
    fun testGetOrderStatusOptions_Empty() {
        val siteModel = SiteModel().apply { id = 1 }

        // Attempt to fetch order status options from database.
        // No options will be available.
        val options = OrderSqlUtils.getOrderStatusOptionsForSite(siteModel)
        assertNotNull(options)
        assertEquals(0, options.size)
    }

    @Test
    fun testGetOrderStatusOption_NotExists() {
        val siteModel = SiteModel().apply { id = 1 }

        // Get the first option from the database by the status key
        val option = OrderSqlUtils.getOrderStatusOptionForSiteByKey(siteModel, "missing")
        assertNull(option)
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

    @Test
    fun testGetOrderShipmentProvidersForOrder() {
        val siteModel = SiteModel().apply { id = 1 }
        val json = UnitTestUtils
                .getStringFromResourceFile(this.javaClass, "wc/order-shipment-providers.json")
        val providers = OrderTestUtils
                .getOrderShipmentProvidersFromJson(json, siteModel.id)
                .toMutableList()
        assertEquals(54, providers.size)

        // Save full list to the database
        var rowsAffected = providers.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentProvider(it) }
        assertEquals(54, rowsAffected)

        // Attempt to save again (should ignore existing entries and add new one)
        providers.add(OrderTestUtils.generateOrderShipmentProvider(siteModel.id))
        rowsAffected = providers.sumBy { OrderSqlUtils.insertOrIgnoreOrderShipmentProvider(it) }
        assertEquals(1, rowsAffected)

        // Get all shipment providers for a single site
        val providersForSite = OrderSqlUtils.getOrderShipmentProvidersForSite(siteModel)
        assertEquals(55, providersForSite.size)
    }

    @Test
    fun testInsertOrderSummaries() {
        // Arrange:
        // - Create the test site so any foreign key dependency works
        // - Create a list of WCOrderSummaryModels for testing
        val site = OrderTestUtils.getAndSaveTestSite()
        val summaryList = OrderTestUtils.getTestOrderSummaryList(site)

        // Act:
        // - Insert all records and verify
        OrderSqlUtils.insertOrUpdateOrderSummaries(summaryList)

        // Assert:
        // - Verify all records were inserted
        val summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId })
        assertEquals(10, summariesDb.size)
    }

    @Test
    fun testUpdateOrderSummaries() {
        // Arrange:
        // - Create the test site so any foreign key dependency works
        // - Create a list of WCOrderSummaryModels for testing
        // - Save first option to the database
        // - Verify first option inserted
        val site = OrderTestUtils.getAndSaveTestSite()
        val summaryList = OrderTestUtils.getTestOrderSummaryList(site)
        val firstOption = summaryList[0]
        OrderSqlUtils.insertOrUpdateOrderSummaries(listOf(firstOption))
        var summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, listOf(firstOption.orderId))
        assertEquals(1, summariesDb.size)
        assertEquals(firstOption.dateCreated, summariesDb[0].dateCreated)
        assertEquals(firstOption.orderId, summariesDb[0].orderId)
        assertEquals(firstOption.localSiteId, summariesDb[0].localSiteId)

        // Act:
        // - Update first option and re-save. This should just update the record in the db
        firstOption.dateCreated = "2019-01-01"
        OrderSqlUtils.insertOrUpdateOrderSummaries(listOf(firstOption))

        // Assert:
        // - Verify the modified property was updated
        summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, listOf(firstOption.orderId))
        assertEquals(1, summariesDb.size)
        assertEquals(firstOption.dateCreated, summariesDb[0].dateCreated)
    }

    @Test
    fun testDeleteOrderSummariesForSite() {
        // Arrange:
        // - Create the test site so any foreign key dependency works
        // - Create a list of WCOrderSummaryModels for testing
        // - Save all order summaries to the db
        // - Verify all orders saved successfully
        val site = OrderTestUtils.getAndSaveTestSite()
        val summaryList = OrderTestUtils.getTestOrderSummaryList(site)
        OrderSqlUtils.insertOrUpdateOrderSummaries(summaryList)
        var summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId })
        assertEquals(10, summariesDb.size)

        // Act:
        // - Delete all summaries
        OrderSqlUtils.deleteOrderSummariesForSite(site)

        // Assert:
        // - Verify all order summaries deleted for the active site
        summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId })
        assertEquals(0, summariesDb.size)
    }

    /**
     * Tests the foreign key relationship between SiteModel and WCOrderSummaryModel.
     */
    @Test
    fun testOrderSummarySiteModelForeignKeyOnDeleteCascade() {
        // Arrange:
        // - Create the test site so any foreign key dependency works
        // - Create a list of WCOrderSummaryModels for testing
        // - Save all order summaries to the db
        // - Verify all orders saved successfully
        val site = OrderTestUtils.getAndSaveTestSite()
        val summaryList = OrderTestUtils.getTestOrderSummaryList(site)
        OrderSqlUtils.insertOrUpdateOrderSummaries(summaryList)
        var summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId })
        assertEquals(10, summariesDb.size)

        // Act:
        // - Delete the site, this should delete all the WCOrderSummaryModel records as well
        TestSiteSqlUtils.siteSqlUtils.deleteSite(site)

        // Assert:
        // - Verify all order summaries for the deleted site have also been deleted
        summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId })
        assertEquals(0, summariesDb.size)
    }

    @Test
    fun testGetOrderSummariesByRemoteIds() {
        // Arrange:
        // - Create a list of 300 WCOrderSummaryModel's
        val site = OrderTestUtils.getAndSaveTestSite()
        val summaryList = OrderTestUtils.getTestOrderSummaryExtendedList(site).sortedByDescending { it.id }

        // Act:
        // - Save 300 order summaries to the db
        OrderSqlUtils.insertOrUpdateOrderSummaries(summaryList)

        // Assert:
        // - Query for the 300 order summary records from the database
        // - Verify all records fetched successfully
        // - Verify list saved to db matches list fetched from db
        val summariesDb = OrderSqlUtils
                .getOrderSummariesForRemoteIds(site, summaryList.map { it.orderId }).sortedBy { it.id }
        assertEquals(300, summariesDb.size)
        assertEquals(summaryList, summariesDb)
    }
}
