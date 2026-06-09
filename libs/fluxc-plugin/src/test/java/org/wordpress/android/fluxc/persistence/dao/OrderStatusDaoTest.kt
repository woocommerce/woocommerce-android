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
import org.wordpress.android.fluxc.model.WCOrderStatusModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
@Suppress("UnitTestNamingRule")
class OrderStatusDaoTest {
    private lateinit var orderStatusDao: OrderStatusDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        orderStatusDao = databaseRule.db.orderStatusDao
    }

    @Test
    fun `when replaceAll called, then inserts order statuses for site`() = runTest {
        // given
        val orderStatuses = listOf(pendingStatus, processingStatus, completedStatus)

        // when
        orderStatusDao.replaceAll(siteId1, orderStatuses)
        val retrievedStatuses = orderStatusDao.getOrderStatusOptions(siteId1)

        // then
        assertThat(retrievedStatuses).containsExactlyInAnyOrderElementsOf(orderStatuses)
    }

    @Test
    fun `given statuses for multiple sites, when getOrderStatusOptions called, then returns only statuses for specific site`() = runTest {
        // given
        val statusesSite1 = listOf(pendingStatus)
        val statusesSite2 = listOf(pendingStatus.copy(siteId = siteId2))

        // when
        orderStatusDao.replaceAll(siteId1, statusesSite1)
        orderStatusDao.replaceAll(siteId2, statusesSite2)
        val retrievedStatusesSite1 = orderStatusDao.getOrderStatusOptions(siteId1)
        val retrievedStatusesSite2 = orderStatusDao.getOrderStatusOptions(siteId2)

        // then
        assertThat(retrievedStatusesSite1).containsExactlyElementsOf(statusesSite1)
        assertThat(retrievedStatusesSite2).containsExactlyElementsOf(statusesSite2)
    }

    @Test
    fun `when getOrderStatusOption called, then returns correct status`() = runTest {
        // when
        orderStatusDao.replaceAll(siteId1, listOf(pendingStatus))
        val retrievedStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "pending")

        // then
        assertThat(retrievedStatus).isEqualTo(pendingStatus)
    }

    @Test
    fun `when getOrderStatusOption called for nonexistent status, then returns null`() = runTest {
        // when
        val retrievedStatus = orderStatusDao.getOrderStatusOption(siteId1, statusKey = "nonexistent")

        // then
        assertThat(retrievedStatus).isNull()
    }

    @Test
    fun `given existing statuses, when replaceAll called, then replaces all statuses for site`() = runTest {
        // given
        val initialStatuses = listOf(pendingStatus, processingStatus)
        val newStatuses = listOf(completedStatus)

        // when
        orderStatusDao.replaceAll(siteId1, initialStatuses)
        val statusesBeforeReplace = orderStatusDao.getOrderStatusOptions(siteId1)

        orderStatusDao.replaceAll(siteId1, newStatuses)
        val statusesAfterReplace = orderStatusDao.getOrderStatusOptions(siteId1)

        // then
        assertThat(statusesAfterReplace)
            .containsExactlyElementsOf(newStatuses)
            .doesNotContainAnyElementsOf(statusesBeforeReplace)
    }

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)

        val pendingStatus = WCOrderStatusModel(
            siteId = siteId1,
            statusKey = "pending",
            label = "Pending payment",
            statusCount = 5
        )

        val processingStatus = WCOrderStatusModel(
            siteId = siteId1,
            statusKey = "processing",
            label = "Processing",
            statusCount = 10
        )

        val completedStatus = WCOrderStatusModel(
            siteId = siteId1,
            statusKey = "completed",
            label = "Completed",
            statusCount = 25
        )
    }
}
