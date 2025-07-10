package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ListAssert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.SiteSqlUtils

@RunWith(RobolectricTestRunner::class)
class OrderSummaryDaoTest {

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    private lateinit var sut: OrderSummaryDao

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        sut = databaseRule.db.orderSummaryDao
    }

    @Test
    fun testUpsertOrderSummaries() = runTest {
        val orderSummaries = createOrderSummariesWith(1, 2, 3)

        sut.upsertOrderSummaries(orderSummaries)

        val result = sut.getOrderSummariesChunked(
            site.localId(),
            listOf(1, 2, 3).asRemoteIds()
        )
        assertThat(result).containsOnlyIds(1, 2, 3)
    }

    @Test
    fun testGetOrderSummariesForRemoteIds() = runTest {
        val orderSummaries = createOrderSummariesWith(1, 2, 3)

        sut.upsertOrderSummaries(orderSummaries)

        val result = sut.getOrderSummariesChunked(site.localId(), listOf(RemoteId(1L), RemoteId(3L)))
        assertThat(result).containsOnlyIds(1, 3)
    }

    @Test
    fun testDeleteOrderSummaryById() = runTest {
        val orderSummaries = createOrderSummariesWith(1, 2)
        sut.upsertOrderSummaries(orderSummaries)

        sut.deleteOrderSummaryById(site.localId(), RemoteId(1L))

        val result = sut.getOrderSummariesChunked(
            site.localId(),
            listOf(1, 2).asRemoteIds()
        )
        assertThat(result).containsOnlyIds(2)
    }

    @Test
    @Ignore("This test is ignored until SiteModel is moved to Room and foreign key constraints are added")
    fun testDeleteSiteDeletesAllOrderSummaries() = runTest {
        val orderSummaries = createOrderSummariesWith(1, 2, 3)
        sut.upsertOrderSummaries(orderSummaries)

        SiteSqlUtils().deleteSite(site)

        val result = sut.getOrderSummariesChunked(
            site.localId(),
            listOf(RemoteId(1L), RemoteId(2L), RemoteId(3L))
        )
        assertThat(result).isEmpty()
    }

    private fun createOrderSummariesWith(vararg ids: Number): List<WCOrderSummaryModel> {
        return ids.map {
            createOrderSummary(site.localId(), RemoteId(it.toLong()))
        }
    }

    private fun createOrderSummary(siteId: LocalId, orderId: RemoteId): WCOrderSummaryModel {
        return WCOrderSummaryModel(
            siteId = siteId,
            orderId = orderId,
            dateCreated = "2023-01-01T10:00:00Z"
        ).apply {
            dateModified = "2023-01-01T10:00:00Z"
        }
    }

    private fun ListAssert<WCOrderSummaryModel?>?.containsOnlyIds(vararg ids: Number) {
        this?.extracting<Long> { it?.orderId?.value }
            ?.containsExactlyInAnyOrder(*ids.map { it.toLong() }.toTypedArray())
    }

    private fun List<Number>.asRemoteIds(): List<RemoteId> {
        return this.map { RemoteId(it.toLong()) }
    }
}
