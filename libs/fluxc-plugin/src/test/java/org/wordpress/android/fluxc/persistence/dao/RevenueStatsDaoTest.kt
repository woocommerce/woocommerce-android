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
import org.wordpress.android.fluxc.model.WCRevenueStatsModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class RevenueStatsDaoTest {
    private lateinit var dao: RevenueStatsDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.revenueStatsDao
    }

    @Test
    fun `when insert revenue stats, then get by site+interval+date returns entity`() = runTest {
        val stat = revenue(
            siteId1,
            StatsGranularity.DAYS.toString(),
            "2024-08-01T00:00:00",
            "2024-08-01T23:59:59",
        )
        dao.insert(stat)

        val retrieved = dao.getBySiteIntervalAndDate(
            siteId = stat.localSiteId,
            granularity = StatsGranularity.valueOf(stat.interval),
            startDate = stat.startDate,
            endDate = stat.endDate
        )

        assertThat(retrieved).isEqualTo(stat)
    }

    @Test
    fun `given multiple sites, when get by site+interval+date called, then returns specific site stats`() = runTest {
        val site1Stat = revenue(
            siteId1,
            StatsGranularity.DAYS.toString(),
            "2024-08-02T00:00:00",
            "2024-08-02T23:59:59",
        )
        dao.insert(site1Stat)
        val site2Stat = revenue(
            siteId2,
            StatsGranularity.WEEKS.toString(),
            "2024-08-02T00:00:00",
            "2024-08-22T23:59:59",
        )
        dao.insert(site2Stat)

        val retrievedSite1 =
            dao.getBySiteIntervalAndDate(
                site1Stat.localSiteId,
                StatsGranularity.valueOf(site1Stat.interval),
                site1Stat.startDate,
                site1Stat.endDate
            )
        val retrievedSite2 =
            dao.getBySiteIntervalAndDate(
                site2Stat.localSiteId,
                StatsGranularity.valueOf(site2Stat.interval),
                site2Stat.startDate,
                site2Stat.endDate
            )

        assertThat(retrievedSite1).isEqualTo(site1Stat)
        assertThat(retrievedSite2).isEqualTo(site2Stat)
    }

    @Test
    fun `when insert with rangeId, then get by site+rangeId returns entity`() = runTest {
        val stat = revenue(
            siteId1,
            StatsGranularity.MONTHS.toString(),
            "2024-08-01T00:00:00",
            "2024-08-31T23:59:59",
            rangeId = "RANGE-123"
        )
        dao.insert(stat)

        val retrieved = dao.getBySiteAndRangeId(
            stat.localSiteId,
            "RANGE-123"
        )

        assertThat(retrieved).isEqualTo(stat)
    }

    @Test
    fun `when insert twice with same (siteId, rangeId), then row is replaced`() = runTest {
        val initialStat = revenue(
            siteId1,
            StatsGranularity.MONTHS.toString(),
            "2024-07-01T00:00:00",
            "2024-07-31T23:59:59",
            rangeId = "RID-1",
            total = "{\"orders_count\":1}"
        )
        val updatedStat = revenue(
            siteId1,
            StatsGranularity.MONTHS.toString(),
            "2024-07-01T00:00:00",
            "2024-07-31T23:59:59",
            rangeId = "RID-1",
            total = "{\"orders_count\":2}"
        )

        dao.insert(initialStat)
        var retrieved = dao.getBySiteAndRangeId(
            siteId1,
            "RID-1"
        )
        assertThat(retrieved).isEqualTo(initialStat)

        dao.insert(updatedStat)
        retrieved = dao.getBySiteAndRangeId(
            siteId1,
            "RID-1"
        )
        assertThat(retrieved).isEqualTo(updatedStat)
    }

    @Suppress("LongParameterList")
    private fun revenue(
        siteId: LocalId,
        interval: String,
        start: String,
        end: String,
        data: String = "[]",
        total: String = "{}",
        rangeId: String = "",
    ) = WCRevenueStatsModel(
        localSiteId = siteId,
        interval = interval,
        startDate = start,
        endDate = end,
        data = data,
        total = total,
        rangeId = rangeId,
    )

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)
    }
}
