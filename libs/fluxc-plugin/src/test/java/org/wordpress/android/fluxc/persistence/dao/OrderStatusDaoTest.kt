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
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class OrderStatusDaoTest {
    private lateinit var orderStatusDao: OrderStatusDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Before
    fun setUp() {
        orderStatusDao = databaseRule.db.orderStatusDao
    }

    @Test
    fun `upsert and retrieve order statuses for site`() = runTest {
        // given
        val orderStatuses = listOf(pendingStatus, processingStatus, completedStatus)

        // when
        orderStatusDao.upsertOrderStatuses(orderStatuses)
        val retrievedStatuses = orderStatusDao.getOrderStatusOptions(siteId1)

        // then
        assertThat(retrievedStatuses).containsExactlyInAnyOrderElementsOf(orderStatuses)
    }

    @Test
    fun `get order status options returns only statuses for specific site`() = runTest {
        // given
        val statusesSite1 = listOf(pendingStatus)
        val statusesSite2 = listOf(pendingStatus.copy(localSiteId = siteId2))

        // when
        orderStatusDao.upsertOrderStatuses(statusesSite1)
        orderStatusDao.upsertOrderStatuses(statusesSite2)
        val retrievedStatusesSite1 = orderStatusDao.getOrderStatusOptions(siteId1)
        val retrievedStatusesSite2 = orderStatusDao.getOrderStatusOptions(siteId2)

        // then
        assertThat(retrievedStatusesSite1).containsExactlyElementsOf(statusesSite1)
        assertThat(retrievedStatusesSite2).containsExactlyElementsOf(statusesSite2)
    }

    @Test
    fun `get specific order status option returns correct status`() = runTest {
        // when
        orderStatusDao.upsertOrderStatuses(listOf(pendingStatus))
        val retrievedStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "pending")

        // then
        assertThat(retrievedStatus).isEqualTo(pendingStatus)
    }

    @Test
    fun `get specific order status option returns null when status doesn't exist`() = runTest {
        // when
        val retrievedStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "nonexistent")

        // then
        assertThat(retrievedStatus).isNull()
    }

    @Test
    fun `upsert updates existing order status`() = runTest {
        // given
        val updatedStatus = pendingStatus.copy(
            label = "Updated Pending payment",
            statusCount = 10
        )

        // when
        orderStatusDao.upsertOrderStatuses(listOf(pendingStatus))
        val initialStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "pending")
        assertThat(initialStatus).isEqualTo(pendingStatus)

        orderStatusDao.upsertOrderStatuses(listOf(updatedStatus))
        val finalStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "pending")

        // then
        assertThat(finalStatus).isEqualTo(updatedStatus)
    }

    @Test
    fun `delete order statuses removes specified statuses`() = runTest {
        // when
        orderStatusDao.upsertOrderStatuses(listOf(pendingStatus, processingStatus, completedStatus))

        orderStatusDao.deleteOrderStatuses(listOf(pendingStatus, completedStatus))
        val remainingStatuses = orderStatusDao.getOrderStatusOptions(siteId1)

        // then
        assertThat(remainingStatuses).containsExactly(processingStatus)
    }

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)

        val pendingStatus = WCOrderStatusModel(
            localSiteId = siteId1,
            statusKey = "pending",
            label = "Pending payment",
            statusCount = 5
        )

        val processingStatus = WCOrderStatusModel(
            localSiteId = siteId1,
            statusKey = "processing",
            label = "Processing",
            statusCount = 10
        )

        val completedStatus = WCOrderStatusModel(
            localSiteId = siteId1,
            statusKey = "completed",
            label = "Completed",
            statusCount = 25
        )
    }
}
