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
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class NewVisitorStatsDaoTest {
    private lateinit var dao: NewVisitorStatsDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.newVisitorStatsDao
    }

    @Test
    fun `when replace stat, then get stat returns by quantity and date`() = runTest {
        val stat = statDay1Site1
        dao.replaceStat(stat)

        val retrieved = dao.getStat(
            stat.localSiteId,
            StatsGranularity.DAYS,
            stat.quantity,
            stat.date
        )

        assertThat(retrieved).isEqualTo(stat)
    }

    @Test
    fun `when replace stat again for same site, then previous stat is deleted`() = runTest {
        // Replace first stat
        dao.replaceStat(statDay1Site1)
        // Now replace a second stat for same site (should clear stat rows for that site)
        dao.replaceStat(statDay2Site1)

        // First one should not be found anymore
        val first = dao.getStat(
            siteId1,
            StatsGranularity.DAYS,
            statDay1Site1.quantity,
            statDay1Site1.date
        )
        // Second one should be present
        val second = dao.getStat(
            siteId1,
            StatsGranularity.DAYS,
            statDay2Site1.quantity,
            statDay2Site1.date
        )

        assertThat(first).isNull()
        assertThat(second).isEqualTo(statDay2Site1)
    }

    companion object {
        private val siteId1 = LocalId(1)

        // Minimal valid payloads for fields and data (stringified JSON)
        private const val FIELDS = "[\"period\",\"visitors\"]"
        private const val DATA = "[[\"2019-08-01\",1]]"

        private val statDay1Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-10",
            startDate = "2019-08-10",
            endDate = "2019-08-10",
            quantity = "1",
            fields = FIELDS,
            data = DATA
        )
        private val statDay2Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-11",
            startDate = "2019-08-11",
            endDate = "2019-08-11",
            quantity = "1",
            fields = FIELDS,
            data = DATA
        )
    }
}
