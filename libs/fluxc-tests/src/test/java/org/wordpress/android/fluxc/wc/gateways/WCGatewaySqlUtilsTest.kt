package org.wordpress.android.fluxc.wc.gateways

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils
import org.wordpress.android.fluxc.persistence.WCGatewaySqlUtils.GatewaysTable
import org.wordpress.android.fluxc.persistence.WellSqlConfig
import org.wordpress.android.fluxc.wc.gateways.GatewayTestFixtures.gatewaysEntities
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGatewaySqlUtilsTest {
    private val site = SiteModel().apply { id = 321 }

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(GatewaysTable::class.java),
                WellSqlConfig.ADDON_WOOCOMMERCE)
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun `test gateway insert`() {
        WCGatewaySqlUtils.insertOrUpdate(site, gatewaysEntities)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertEquals(2, gateways.size)
        assertEquals(gatewaysEntities, gateways)
    }

    @Test
    fun `test gateway update`() {
        val response = gatewaysEntities.first()
        WCGatewaySqlUtils.insertOrUpdate(site, response)
        val gateway = WCGatewaySqlUtils.selectGateway(site, response.gatewayId)!!
        assertEquals(response, gateway)

        val newData = "New Data"
        WCGatewaySqlUtils.insertOrUpdate(site, response.copy(data = newData))
        val updatedGateway = WCGatewaySqlUtils.selectGateway(site, response.gatewayId)!!
        assertEquals(newData, updatedGateway.data)
    }

    @Test
    fun `test select`() {
        WCGatewaySqlUtils.insertOrUpdate(site, gatewaysEntities)

        val gateway = WCGatewaySqlUtils.selectGateway(site, "stripe")
        assertEquals(gatewaysEntities[1], gateway)
    }

    @Test
    fun `test select empty result`() {
        val newSiteId = 123
        val newSite = SiteModel().apply { id = newSiteId }
        val newGatewayEntities = listOf(
            gatewaysEntities[0].copy(localSiteId = newSiteId),
            gatewaysEntities[1].copy(localSiteId = newSiteId)
        )
        WCGatewaySqlUtils.insertOrUpdate(newSite, newGatewayEntities)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertTrue(gateways.isEmpty())

        val gateway = WCGatewaySqlUtils.selectGateway(site, newGatewayEntities.first().gatewayId)
        assertNull(gateway)
    }
}
