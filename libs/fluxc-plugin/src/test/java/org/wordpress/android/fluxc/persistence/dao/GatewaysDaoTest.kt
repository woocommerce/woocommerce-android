package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.GatewayEntity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("UnitTestNamingRule")
class GatewaysDaoTest {
    private lateinit var dao: GatewaysDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.gatewaysDao
    }

    @Test
    fun `given multiple sites, when getGatewaysForSite called, then returns specific site gateways`() = runTest {
        val site1Gateways = listOf(
            entity(siteId1, gatewayIdCod, dataCod),
            entity(siteId1, gatewayIdStripe, dataStripe)
        )
        val site2Gateways = listOf(
            entity(siteId2, gatewayIdCod, dataCodSite2)
        )
        dao.replaceAllForSite(siteId1, site1Gateways)
        dao.replaceAllForSite(siteId2, site2Gateways)

        val retrievedSite1 = dao.getGatewaysForSite(siteId1)
        val retrievedSite2 = dao.getGatewaysForSite(siteId2)

        assertThat(retrievedSite1).containsExactlyInAnyOrderElementsOf(site1Gateways)
        assertThat(retrievedSite2).containsExactlyInAnyOrderElementsOf(site2Gateways)
    }

    @Test
    fun `when getGateway called, then returns correct gateway for site and id`() = runTest {
        val site1Gateways = listOf(
            entity(siteId1, gatewayIdCod, dataCod),
            entity(siteId1, gatewayIdStripe, dataStripe)
        )
        dao.replaceAllForSite(siteId1, site1Gateways)

        val found = dao.getGateway(siteId1, gatewayIdStripe)
        val notFoundDifferentSite = dao.getGateway(siteId2, gatewayIdStripe)
        val notFoundDifferentId = dao.getGateway(siteId1, "does_not_exist")

        assertThat(found).isEqualTo(entity(siteId1, gatewayIdStripe, dataStripe))
        assertThat(notFoundDifferentSite).isNull()
        assertThat(notFoundDifferentId).isNull()
    }

    @Test
    fun `when replaceAllForSite called, then inserts gateways for site`() = runTest {
        val gateways = listOf(
            entity(siteId1, gatewayIdCod, dataCod),
            entity(siteId1, gatewayIdStripe, dataStripe)
        )

        dao.replaceAllForSite(siteId1, gateways)
        val retrieved = dao.getGatewaysForSite(siteId1)

        assertThat(retrieved).containsExactlyInAnyOrderElementsOf(gateways)
    }

    @Test
    fun `given existing gateways, when replaceAllForSite called, then deletes gateways for site first`() = runTest {
        val existing = listOf(
            entity(siteId1, gatewayIdCod, dataCod),
            entity(siteId1, gatewayIdStripe, dataStripe)
        )
        dao.replaceAllForSite(siteId1, existing)
        val replacement = listOf(
            entity(siteId1, gatewayIdCod, dataCodUpdated) // only one, data changed
        )

        dao.replaceAllForSite(siteId1, replacement)
        val retrieved = dao.getGatewaysForSite(siteId1)

        assertThat(retrieved).containsExactlyElementsOf(replacement)
    }

    @Test
    fun `when upsertGateway called twice with same key, then row is updated`() = runTest {
        // Insert initial gateway
        val initial = entity(siteId1, gatewayIdCod, dataCod)
        dao.upsertGateway(initial)
        // Insert another gateway to ensure others aren't affected
        val other = entity(siteId1, gatewayIdStripe, dataStripe)
        dao.upsertGateway(other)

        var retrieved = dao.getGateway(siteId1, gatewayIdCod)
        assertThat(retrieved).isEqualTo(initial)
        val retrievedOtherBefore = dao.getGateway(siteId1, gatewayIdStripe)
        assertThat(retrievedOtherBefore).isEqualTo(other)

        // Upsert with updated data for the same (siteId, gatewayId)
        val updated = entity(siteId1, gatewayIdCod, dataCodUpdated)
        dao.upsertGateway(updated)

        // Verify the gateway was updated, and the other gateway is unchanged
        retrieved = dao.getGateway(siteId1, gatewayIdCod)
        assertThat(retrieved).isEqualTo(updated)
        val retrievedOtherAfter = dao.getGateway(siteId1, gatewayIdStripe)
        assertThat(retrievedOtherAfter).isEqualTo(other)
    }

    private fun entity(siteId: LocalId, gatewayId: String, data: String) =
        GatewayEntity(siteId = siteId, gatewayId = gatewayId, data = data)

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)
        private const val gatewayIdCod = "cod"
        private const val gatewayIdStripe = "stripe"

        // Minimal JSON payloads (structure isn't validated by DAO, so any string is fine)
        private const val dataCod = "{\"id\":\"cod\",\"enabled\":true}"
        private const val dataCodUpdated = "{\"id\":\"cod\",\"enabled\":false}"
        private const val dataStripe = "{\"id\":\"stripe\",\"enabled\":true}"
        private const val dataCodSite2 = "{\"id\":\"cod\",\"enabled\":true,\"site\":2}"
    }
}
