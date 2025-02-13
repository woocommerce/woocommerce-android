package org.wordpress.android.fluxc.wc.shippinglabels

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.shippinglabels.WCShippingLabelModel
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WCShippingLabelSqlUtils
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCShippingLabelSqlUtilsTest {
    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    private val orderId = 25L

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(SiteModel::class.java, WCShippingLabelModel::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()

        // Insert the site into the db so it's available later for shipping labels
        SiteSqlUtils().insertOrUpdateSite(site)
    }

    @Test
    fun testInsertOrUpdateShippingLabelForOrder() {
        val shippingLabel = WCShippingLabelTestUtils.generateShippingLabelList(site.id, orderId)[0]
        assertNotNull(shippingLabel)

        // Test inserting a shipping label for an order
        var rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabel(shippingLabel)
        assertEquals(1, rowsAffected)
        var savedShippingLabels = WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)
        assertEquals(savedShippingLabels.size, 1)
        assertEquals(savedShippingLabels[0].localSiteId, shippingLabel.localSiteId)
        assertEquals(savedShippingLabels[0].remoteOrderId, shippingLabel.remoteOrderId)
        assertEquals(savedShippingLabels[0].remoteShippingLabelId, shippingLabel.remoteShippingLabelId)
        assertEquals(savedShippingLabels[0].serviceName, shippingLabel.serviceName)
        assertNotNull(savedShippingLabels[0].refund)

        // Test updating the same shipping label
        shippingLabel.apply {
            serviceName = "Test service name"
            refund = ""
        }
        rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabel(shippingLabel)
        assertEquals(1, rowsAffected)
        savedShippingLabels = WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)
        assertEquals(savedShippingLabels.size, 1)
        assertEquals(savedShippingLabels[0].localSiteId, shippingLabel.localSiteId)
        assertEquals(savedShippingLabels[0].remoteOrderId, shippingLabel.remoteOrderId)
        assertEquals(savedShippingLabels[0].remoteShippingLabelId, shippingLabel.remoteShippingLabelId)
        assertEquals(savedShippingLabels[0].serviceName, shippingLabel.serviceName)
        assertEquals(savedShippingLabels[0].refund, shippingLabel.refund)
    }

    @Test
    fun testInsertOrUpdateShippingLabelListForOrder() {
        val shippingLabels = WCShippingLabelTestUtils.generateShippingLabelList(site.id, orderId)
        assertNotNull(shippingLabels)

        // Insert shipping label list
        val rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
        assertEquals(shippingLabels.size, rowsAffected)
    }

    @Test
    fun testGetShippingLabelsForOrder() {
        val shippingLabels = WCShippingLabelTestUtils.generateShippingLabelList(site.id, orderId)
        assertTrue(shippingLabels.isNotEmpty())

        // Insert shipping label list
        val rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
        assertEquals(shippingLabels.size, rowsAffected)

        // Get shipping label list for site and order and verify
        val savedShippingLabelListExists = WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)
        assertEquals(shippingLabels.size, savedShippingLabelListExists.size)
        assertEquals(shippingLabels[0].getProductNameList(), savedShippingLabelListExists[0].getProductNameList())
        assertEquals(shippingLabels[0].getProductIdsList(), savedShippingLabelListExists[0].getProductIdsList())

        // Get shipping label list for a site that does not exist
        val nonExistingSite = SiteModel().apply { id = 400 }
        val savedShippingLabelList = WCShippingLabelSqlUtils.getShippingClassesForOrder(nonExistingSite.id, orderId)
        assertEquals(0, savedShippingLabelList.size)

        // Get shipping label list for an order that does not exist
        val nonExistingOrderId = 45L
        val nonExistentOrderShippingLabelList =
                WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, nonExistingOrderId)
        assertEquals(0, nonExistentOrderShippingLabelList.size)
    }

    @Test
    fun testGetShippingLabelsById() {
        val shippingLabelId = 1L
        val shippingLabels = WCShippingLabelTestUtils.generateShippingLabelList(
                site.id, orderId, shippingLabelId
        )
        assertTrue(shippingLabels.isNotEmpty())

        // Insert shipping label list
        val rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
        assertEquals(shippingLabels.size, rowsAffected)

        // Get shipping label list by id and verify
        val savedShippingLabelExists = WCShippingLabelSqlUtils.getShippingLabelById(
                site.id, orderId, shippingLabelId + 1
        )
        assertEquals(shippingLabels[0].remoteOrderId, savedShippingLabelExists?.remoteOrderId)
        assertEquals(shippingLabels[0].remoteShippingLabelId, savedShippingLabelExists?.remoteShippingLabelId)
        assertEquals(shippingLabels[0].localSiteId, savedShippingLabelExists?.localSiteId)

        // Get shipping label for am id that does not exist
        val savedShippingLabel = WCShippingLabelSqlUtils.getShippingLabelById(site.id, orderId, 100)
        assertNull(savedShippingLabel)
    }

    @Test
    fun testDeleteShippingLabelListForOrder() {
        val shippingLabels = WCShippingLabelTestUtils.generateShippingLabelList(site.id, orderId)

        var rowsAffected = WCShippingLabelSqlUtils.insertOrUpdateShippingLabels(shippingLabels)
        assertEquals(shippingLabels.size, rowsAffected)

        // Verify shipping label list inserted
        var savedShippingLabelList = WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)
        assertEquals(shippingLabels.size, savedShippingLabelList.size)

        // Delete shipping label list for order and verify
        rowsAffected = WCShippingLabelSqlUtils.deleteShippingLabelsForOrder(orderId)
        assertEquals(shippingLabels.size, rowsAffected)
        savedShippingLabelList = WCShippingLabelSqlUtils.getShippingClassesForOrder(site.id, orderId)
        assertEquals(0, savedShippingLabelList.size)
    }
}
