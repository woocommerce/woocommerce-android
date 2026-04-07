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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.WCOrderSummaryModel
import org.wordpress.android.fluxc.persistence.AccountMapper
import org.wordpress.android.fluxc.persistence.AccountStorePersistence
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.persistence.SiteSqlUtils
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.utils.FakeOrderSummaryGenerator.asOrderSummaries

@RunWith(RobolectricTestRunner::class)
class OrderSummaryDaoTest {

    @Rule
    @JvmField
    val wcDatabaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    private lateinit var sut: OrderSummaryDao
    private lateinit var siteSqlUtils: SiteSqlUtils

    val site = SiteModel().apply {
        email = "test@example.org"
        name = "Test Site"
        siteId = 24
    }

    @Before
    fun setUp() {
        sut = wcDatabaseRule.db.orderSummaryDao
        siteSqlUtils = SiteSqlUtils(AccountStorePersistence(wpDatabaseRule.db, AccountMapper()))
    }

    @Test
    fun testGetOrderSummariesForRemoteIds() = runTest {
        val orderSummaries = (1..3).asOrderSummaries(site.localId())

        sut.upsertOrderSummaries(orderSummaries)

        val result = sut.getOrderSummaries(site.localId(), listOf(RemoteId(1L), RemoteId(3L)))
        assertThat(result).containsOnlyIds(1, 3)
    }

    @Test
    fun testDeleteOrderSummaryById() = runTest {
        val orderSummaries = (1..2).asOrderSummaries(site.localId())
        sut.upsertOrderSummaries(orderSummaries)

        sut.deleteOrderSummaryById(site.localId(), RemoteId(1L))

        val result = sut.getOrderSummaries(
            site.localId(),
            listOf(1, 2).asRemoteIds()
        )
        assertThat(result).containsOnlyIds(2)
    }

    @Test
    @Ignore("This test is ignored until SiteModel is moved to Room and foreign key constraints are added")
    fun testDeleteSiteDeletesAllOrderSummaries() = runTest {
        val orderSummaries = (1..3).asOrderSummaries(site.localId())
        sut.upsertOrderSummaries(orderSummaries)

        siteSqlUtils.deleteSite(site)

        val result = sut.getOrderSummaries(
            site.localId(),
            listOf(RemoteId(1L), RemoteId(2L), RemoteId(3L))
        )
        assertThat(result).isEmpty()
    }

    @Test
    fun testQueryingLargeNumberOfOrderSummaries() = runTest {
        val orderSummaries = (1..SQLITE_MAX_VARIABLE_NUMBER).asOrderSummaries(site.localId())
        sut.upsertOrderSummaries(orderSummaries)

        val result = sut.getOrderSummaries(
            site.localId(),
            orderSummaries.map(WCOrderSummaryModel::orderId)
        )

        assertThat(result).containsExactlyInAnyOrderElementsOf(orderSummaries)
    }

    private fun ListAssert<WCOrderSummaryModel?>?.containsOnlyIds(vararg ids: Number) {
        this?.extracting<Long> { it?.orderId?.value }
            ?.containsExactlyInAnyOrder(*ids.map { it.toLong() }.toTypedArray())
    }

    private fun List<Number>.asRemoteIds(): List<RemoteId> {
        return this.map { RemoteId(it.toLong()) }
    }

    private companion object {
        private const val SQLITE_MAX_VARIABLE_NUMBER = 32766
    }
}
