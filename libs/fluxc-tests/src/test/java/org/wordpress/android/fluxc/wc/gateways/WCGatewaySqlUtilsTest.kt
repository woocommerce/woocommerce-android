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
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WCGatewaySqlUtilsTest {
    private val site = SiteModel().apply { id = 2 }

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
        WCGatewaySqlUtils.insertOrUpdate(site, GATEWAYS_ENTITIES)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertEquals(2, gateways.size)
        assertEquals(GATEWAYS_ENTITIES, gateways)
    }

    @Test
    fun `test gateway update`() {
        val response = GATEWAYS_ENTITIES.first()
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
        WCGatewaySqlUtils.insertOrUpdate(site, GATEWAYS_ENTITIES)

        val gateway = WCGatewaySqlUtils.selectGateway(site, "stripe")
        assertEquals(GATEWAYS_ENTITIES[1], gateway)
    }

    @Test
    fun `test select empty result`() {
        val newSiteId = 3
        val newSite = SiteModel().apply { id = newSiteId }
        val newGatewayEntities = listOf(
            GATEWAYS_ENTITIES[0].copy(localSiteId = newSiteId),
            GATEWAYS_ENTITIES[1].copy(localSiteId = newSiteId)
        )
        WCGatewaySqlUtils.insertOrUpdate(newSite, newGatewayEntities)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertTrue(gateways.isEmpty())

        val gateway = WCGatewaySqlUtils.selectGateway(site, newGatewayEntities.first().gatewayId)
        assertNull(gateway)
    }
}
