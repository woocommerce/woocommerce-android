package org.wordpress.android.fluxc.persistence.dao

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.model.settings.AnalyticsScheduledImportSettingEntity
import org.wordpress.android.fluxc.persistence.DatabaseTestRule

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class AnalyticsScheduledImportDaoTest {
    private lateinit var sut: AnalyticsScheduledImportDao

    private val siteId = LocalId(1)

    @Rule
    @JvmField
    val databaseRule = DatabaseTestRule(ApplicationProvider.getApplicationContext<Application>())

    @Before
    fun setup() {
        sut = databaseRule.db.analyticsScheduledImportDao
    }

    @Test
    fun `given no cached setting, when observed, then null is emitted`() = runTest {
        val result = sut.observeSetting(siteId).first()

        assertThat(result).isNull()
    }

    @Test
    fun `given a cached setting, when observed, then it is emitted`() = runTest {
        val setting = AnalyticsScheduledImportSettingEntity(localSiteId = siteId, isEnabled = true)

        sut.insertOrUpdate(setting)
        val result = sut.observeSetting(siteId).first()

        assertThat(result).isEqualTo(setting)
    }

    @Test
    fun `given an existing setting, when inserted again, then the value is replaced`() = runTest {
        sut.insertOrUpdate(AnalyticsScheduledImportSettingEntity(localSiteId = siteId, isEnabled = true))

        sut.insertOrUpdate(AnalyticsScheduledImportSettingEntity(localSiteId = siteId, isEnabled = false))
        val result = sut.observeSetting(siteId).first()

        assertThat(result?.isEnabled).isFalse()
    }

    @Test
    fun `given settings for multiple sites, when observed, then each site is isolated`() = runTest {
        val otherSiteId = LocalId(2)
        sut.insertOrUpdate(AnalyticsScheduledImportSettingEntity(localSiteId = siteId, isEnabled = true))
        sut.insertOrUpdate(AnalyticsScheduledImportSettingEntity(localSiteId = otherSiteId, isEnabled = false))

        assertThat(sut.observeSetting(siteId).first()?.isEnabled).isTrue()
        assertThat(sut.observeSetting(otherSiteId).first()?.isEnabled).isFalse()
    }
}
