package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.wc.order.OrderTestUtils

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class OrderFulfillmentDaoTest {
    private lateinit var orderFulfillmentDao: OrderFulfillmentDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private val defaultSiteId = LocalId(6)
    private val defaultOrderId = 12L

    @Before
    fun setUp() {
        orderFulfillmentDao = databaseRule.db.orderFulfillmentDao
    }

    @Test
    fun `when fulfillment is upserted, then it can be retrieved`() = runTest {
        val fulfillment = OrderTestUtils.generateOrderFulfillment(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId
        )

        orderFulfillmentDao.upsertOrderFulfillment(fulfillment)

        val result = orderFulfillmentDao.getOrderFulfillments(defaultSiteId, RemoteId(defaultOrderId))

        assertThat(result).containsExactly(fulfillment)
    }

    @Test
    fun `when fulfillment is updated, then changes are persisted`() = runTest {
        val fulfillment = OrderTestUtils.generateOrderFulfillment(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId
        )
        orderFulfillmentDao.upsertOrderFulfillment(fulfillment)

        val updatedFulfillment = fulfillment.copy(
            shipmentProvider = "fedex",
            trackingUrl = "https://fedex.com/track"
        )
        orderFulfillmentDao.upsertOrderFulfillment(updatedFulfillment)

        val result = orderFulfillmentDao.getOrderFulfillments(defaultSiteId, RemoteId(defaultOrderId))

        assertThat(result).containsExactly(updatedFulfillment)
    }

    @Test
    fun `when fulfillment is deleted, then it is removed from database`() = runTest {
        val fulfillment = OrderTestUtils.generateOrderFulfillment(
            siteId = defaultSiteId.value,
            orderId = defaultOrderId
        )
        orderFulfillmentDao.upsertOrderFulfillment(fulfillment)

        orderFulfillmentDao.deleteOrderFulfillment(fulfillment)

        val result = orderFulfillmentDao.getOrderFulfillments(defaultSiteId, RemoteId(defaultOrderId))

        assertThat(result).isEmpty()
    }
}
