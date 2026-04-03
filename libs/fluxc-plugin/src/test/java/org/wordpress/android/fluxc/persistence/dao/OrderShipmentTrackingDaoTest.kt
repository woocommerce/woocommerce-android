package org.wordpress.android.fluxc.persistence.dao

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.WCOrderShipmentTrackingModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@RunWith(RobolectricTestRunner::class)
class OrderShipmentTrackingDaoTest {

    private lateinit var orderShipmentTrackingDao: OrderShipmentTrackingDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    private val defaultSiteId = LocalId(6)
    private val defaultOrderId = 12L

    @Before
    fun setUp() {
        orderShipmentTrackingDao = databaseRule.db.orderShipmentTrackingDao
    }

    @Test
    fun `when shipment tracking is upserted, then it can be retrieved`() = runTest {
        // given
        val tracking = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track123",
            trackingNumber = "1234567890"
        )
        orderShipmentTrackingDao.upsertShipmentTracking(tracking)

        // when
        val result = orderShipmentTrackingDao.getShipmentTrackings(defaultSiteId, RemoteId(defaultOrderId))

        // then
        assertThat(result).containsExactly(tracking)
    }

    @Test
    fun `when multiple shipment trackings are upserted, then they can be retrieved`() = runTest {
        // given
        val trackings = List(3) { id ->
            generateShipmentTracking(
                siteId = defaultSiteId.value,
                orderId = defaultOrderId,
                remoteTrackingId = "track$id",
                trackingNumber = "12345$id"
            )
        }
        trackings.forEach { orderShipmentTrackingDao.upsertShipmentTracking(it) }

        // when
        val result = orderShipmentTrackingDao.getShipmentTrackings(defaultSiteId, RemoteId(defaultOrderId))

        // then
        assertThat(result).containsExactlyInAnyOrderElementsOf(trackings)
    }

    @Test
    fun `when shipment tracking exists, then it can be retrieved by tracking number`() = runTest {
        // given
        val trackingNumber = "1234567890"
        val tracking = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track123",
            trackingNumber = trackingNumber
        )
        orderShipmentTrackingDao.upsertShipmentTracking(tracking)

        // when
        val retrievedTracking = orderShipmentTrackingDao.getShipmentTrackingByNumber(
            defaultSiteId,
            RemoteId(defaultOrderId),
            trackingNumber
        )
        val nonExistentTracking = orderShipmentTrackingDao.getShipmentTrackingByNumber(
            defaultSiteId,
            RemoteId(defaultOrderId),
            "nonexistent"
        )

        // then
        assertThat(retrievedTracking).isEqualTo(tracking)
        assertThat(nonExistentTracking).isNull()
    }

    @Test
    fun `when shipment tracking is updated, then changes are persisted`() = runTest {
        // given
        val tracking = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track123",
            trackingNumber = "1234567890",
            trackingProvider = "UPS"
        )
        orderShipmentTrackingDao.upsertShipmentTracking(tracking)

        // when
        val updatedTracking = tracking.copy(
            trackingProvider = "FedEx",
            trackingLink = "https://fedex.com/tracking"
        )
        orderShipmentTrackingDao.upsertShipmentTracking(updatedTracking)
        val retrievedTrackings = orderShipmentTrackingDao.getShipmentTrackings(defaultSiteId, RemoteId(defaultOrderId))

        // then
        assertThat(retrievedTrackings).containsExactly(updatedTracking)
    }

    @Test
    fun `when shipment tracking is deleted, then it is removed from database`() = runTest {
        // given
        val tracking = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track123",
            trackingNumber = "1234567890"
        )
        orderShipmentTrackingDao.upsertShipmentTracking(tracking)

        // when
        orderShipmentTrackingDao.deleteShipmentTracking(tracking)
        val retrievedTrackings = orderShipmentTrackingDao.getShipmentTrackings(defaultSiteId, RemoteId(defaultOrderId))

        // then
        assertThat(retrievedTrackings).isEmpty()
    }

    @Test
    fun `when shipment trackings are retrieved, then they are ordered by date shipped DESC`() = runTest {
        // given
        val tracking1 = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track1",
            trackingNumber = "111",
            dateShipped = "2024-01-01"
        )
        val tracking2 = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track2",
            trackingNumber = "222",
            dateShipped = "2024-01-03"
        )
        val tracking3 = generateShipmentTracking(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId,
            remoteTrackingId = "track3",
            trackingNumber = "333",
            dateShipped = "2024-01-02"
        )

        // Insert in random order
        orderShipmentTrackingDao.upsertShipmentTracking(tracking1)
        orderShipmentTrackingDao.upsertShipmentTracking(tracking3)
        orderShipmentTrackingDao.upsertShipmentTracking(tracking2)

        // when
        val result = orderShipmentTrackingDao.getShipmentTrackings(defaultSiteId, RemoteId(defaultOrderId))

        // then - should be ordered by dateShipped DESC
        assertThat(result).containsExactly(tracking2, tracking3, tracking1)
    }

    private fun generateShipmentTracking(
        siteId: Int,
        orderId: Long,
        remoteTrackingId: String,
        trackingNumber: String,
        trackingProvider: String = "USPS",
        trackingLink: String = "https://usps.com/tracking",
        dateShipped: String = "2024-01-01"
    ) = WCOrderShipmentTrackingModel(
        localSiteId = LocalId(siteId),
        orderId = RemoteId(orderId),
        remoteTrackingId = remoteTrackingId,
        trackingNumber = trackingNumber,
        trackingProvider = trackingProvider,
        trackingLink = trackingLink,
        dateShipped = dateShipped
    )
}
