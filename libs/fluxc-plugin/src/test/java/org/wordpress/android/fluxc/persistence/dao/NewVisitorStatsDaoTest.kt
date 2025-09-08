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
import org.wordpress.android.fluxc.model.WCNewVisitorStatsModel
import org.wordpress.android.fluxc.persistence.DatabaseTestRule
import org.wordpress.android.fluxc.store.WCStatsStore.StatsGranularity

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class NewVisitorStatsDaoTest {
    private lateinit var dao: NewVisitorStatsDao

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Before
    fun setUp() {
        dao = databaseRule.db.newVisitorStatsDao
    }

    @Test
    fun `when insert default stat, then getDefaultStat returns it`() = runTest {
        val default = defaultDay1Site1
        dao.insertOrUpdateStat(default)

        val retrieved = dao.getDefaultStat(default.localSiteId, StatsGranularity.DAYS)

        assertThat(retrieved).isEqualTo(default)
    }

    @Test
    fun `given multiple sites & granularities, when insert stats, then getDefaultStat returns correct row`() = runTest {
        // Insert different combinations
        val defaultSite1Day = defaultDay1Site1
        dao.insertOrUpdateStat(defaultSite1Day)
        val defaultSite1Week = defaultWeek1Site1
        dao.insertOrUpdateStat(defaultSite1Week)
        val defaultSite2Day = defaultDay1Site2
        dao.insertOrUpdateStat(defaultSite2Day)

        val retrievedSite1Day = dao.getDefaultStat(siteId1, StatsGranularity.DAYS)
        val retrievedSite1Week = dao.getDefaultStat(siteId1, StatsGranularity.WEEKS)
        val retrievedSite2Day = dao.getDefaultStat(siteId2, StatsGranularity.DAYS)

        assertThat(retrievedSite1Day).isEqualTo(defaultSite1Day)
        assertThat(retrievedSite1Week).isEqualTo(defaultSite1Week)
        assertThat(retrievedSite2Day).isEqualTo(defaultSite2Day)
    }

    @Test
    fun `when insert custom stat, then getCustomStat returns by quantity and date`() = runTest {
        val custom = customDay1Site1
        dao.insertOrUpdateStat(custom)

        val retrieved = dao.getCustomStat(
            custom.localSiteId,
            StatsGranularity.DAYS,
            custom.quantity,
            custom.date
        )

        assertThat(retrieved).isEqualTo(custom)
    }

    @Test
    fun `when insert custom stat again for same site, then previous custom is deleted`() = runTest {
        // Insert first custom
        dao.insertOrUpdateStat(customDay1Site1)
        // Now insert a second custom for same site (should clear previous custom rows for that site)
        dao.insertOrUpdateStat(customDay2Site1)

        // First one should not be found anymore
        val first = dao.getCustomStat(
            siteId1,
            StatsGranularity.DAYS,
            customDay1Site1.quantity,
            customDay1Site1.date
        )
        // Second one should be present
        val second = dao.getCustomStat(
            siteId1,
            StatsGranularity.DAYS,
            customDay2Site1.quantity,
            customDay2Site1.date
        )

        assertThat(first).isNull()
        assertThat(second).isEqualTo(customDay2Site1)
    }

    @Test
    fun `when insert default stat again for same site, then previous custom is not deleted`() = runTest {
        // Have a custom row for site 1
        dao.insertOrUpdateStat(customDay1Site1)
        // Insert a default row for same site and granularity
        dao.insertOrUpdateStat(defaultDay1Site1)

        // Custom should still be there
        val custom = dao.getCustomStat(
            siteId1,
            StatsGranularity.DAYS,
            customDay1Site1.quantity,
            customDay1Site1.date
        )
        val default = dao.getDefaultStat(siteId1, StatsGranularity.DAYS)

        assertThat(custom).isEqualTo(customDay1Site1)
        assertThat(default).isEqualTo(defaultDay1Site1)
    }

    companion object {
        private val siteId1 = LocalId(1)
        private val siteId2 = LocalId(2)

        // Minimal valid payloads for fields and data (stringified JSON)
        private const val FIELDS = "[\"period\",\"visitors\"]"
        private const val DATA = "[[\"2019-08-01\",1]]"

        // Default stats rows (isCustomField = false)
        private val defaultDay1Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-01",
            startDate = "",
            endDate = "",
            quantity = "30",
            isCustomField = false,
            fields = FIELDS,
            data = DATA
        )
        private val defaultWeek1Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.WEEKS.toString(),
            date = "2019-08-01",
            startDate = "",
            endDate = "",
            quantity = "12",
            isCustomField = false,
            fields = FIELDS,
            data = DATA
        )
        private val defaultDay1Site2 = WCNewVisitorStatsModel(
            localSiteId = siteId2,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-01",
            startDate = "",
            endDate = "",
            quantity = "30",
            isCustomField = false,
            fields = FIELDS,
            data = DATA
        )

        // Custom stats rows (isCustomField = true)
        private val customDay1Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-10",
            startDate = "2019-08-10",
            endDate = "2019-08-10",
            quantity = "1",
            isCustomField = true,
            fields = FIELDS,
            data = DATA
        )
        private val customDay2Site1 = WCNewVisitorStatsModel(
            localSiteId = siteId1,
            granularity = StatsGranularity.DAYS.toString(),
            date = "2019-08-11",
            startDate = "2019-08-11",
            endDate = "2019-08-11",
            quantity = "1",
            isCustomField = true,
            fields = FIELDS,
            data = DATA
        )
    }
}
