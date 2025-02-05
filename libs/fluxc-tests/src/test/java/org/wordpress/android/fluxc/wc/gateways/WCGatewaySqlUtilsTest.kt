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
        WCGatewaySqlUtils.insertOrUpdate(site, GATEWAYS_RESPONSE)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertEquals(2, gateways.size)
        assertEquals(GATEWAYS_RESPONSE, gateways)
    }

    @Test
    fun `test gateway update`() {
        val response = GATEWAYS_RESPONSE.first()
        WCGatewaySqlUtils.insertOrUpdate(site, response)
        val gateway = WCGatewaySqlUtils.selectGateway(site, response.gatewayId)!!
        assertEquals(response, gateway)

        val newTitle = "New title"
        WCGatewaySqlUtils.insertOrUpdate(site, response.copy(title = newTitle))
        val updatedGateway = WCGatewaySqlUtils.selectGateway(site, response.gatewayId)!!
        assertEquals(newTitle, updatedGateway.title)
    }

    @Test
    fun `test select`() {
        WCGatewaySqlUtils.insertOrUpdate(site, GATEWAYS_RESPONSE)

        val gateway = WCGatewaySqlUtils.selectGateway(site, "stripe")
        assertEquals(GATEWAYS_RESPONSE[1], gateway)
    }

    @Test
    fun `test select empty result`() {
        val newSite = SiteModel().apply { id = 3 }
        WCGatewaySqlUtils.insertOrUpdate(newSite, GATEWAYS_RESPONSE)
        val gateways = WCGatewaySqlUtils.selectAllGateways(site)
        assertTrue(gateways.isEmpty())

        val gateway = WCGatewaySqlUtils.selectGateway(site, GATEWAYS_RESPONSE.first().gatewayId)
        assertNull(gateway)
    }
}
