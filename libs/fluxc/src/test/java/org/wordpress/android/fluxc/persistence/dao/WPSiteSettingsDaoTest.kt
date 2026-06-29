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
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.entity.WPSiteSettingsModel
import java.time.DayOfWeek

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class WPSiteSettingsDaoTest {
    private lateinit var dao: WPSiteSettingsDao

    @Rule
    @JvmField
    val databaseRule = WPDatabaseTestRule(ApplicationProvider.getApplicationContext())

    @Before
    fun setUp() {
        dao = databaseRule.db.wpSiteSettingsDao()
    }

    @Test
    fun `given monday start stored, when settings are read, then returns monday for the same site`() = runTest {
        val settings = WPSiteSettingsModel(
            localSiteId = SITE_ID,
            startOfWeek = DayOfWeek.MONDAY
        )

        dao.upsertSiteSettings(settings)

        assertThat(dao.getSiteSettings(SITE_ID)).isEqualTo(settings)
    }

    @Test
    fun `given sunday start stored, when settings are read, then returns sunday for the same site`() = runTest {
        val settings = WPSiteSettingsModel(
            localSiteId = SITE_ID,
            startOfWeek = DayOfWeek.SUNDAY
        )

        dao.upsertSiteSettings(settings)

        assertThat(dao.getSiteSettings(SITE_ID)?.startOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }

    @Test
    fun `given unavailable start stored, when settings are read, then null is preserved`() = runTest {
        val settings = WPSiteSettingsModel(
            localSiteId = SITE_ID,
            startOfWeek = null
        )

        dao.upsertSiteSettings(settings)

        assertThat(dao.getSiteSettings(SITE_ID)?.startOfWeek).isNull()
    }

    @Test
    fun `given settings for multiple sites, when one site is updated, then other site is unchanged`() = runTest {
        dao.upsertSiteSettings(WPSiteSettingsModel(SITE_ID, startOfWeek = DayOfWeek.MONDAY))
        dao.upsertSiteSettings(WPSiteSettingsModel(OTHER_SITE_ID, startOfWeek = DayOfWeek.SUNDAY))

        dao.upsertSiteSettings(WPSiteSettingsModel(SITE_ID, startOfWeek = DayOfWeek.SATURDAY))

        assertThat(dao.getSiteSettings(SITE_ID)?.startOfWeek).isEqualTo(DayOfWeek.SATURDAY)
        assertThat(dao.getSiteSettings(OTHER_SITE_ID)?.startOfWeek).isEqualTo(DayOfWeek.SUNDAY)
    }

    private companion object {
        val SITE_ID = LocalId(1)
        val OTHER_SITE_ID = LocalId(2)
    }
}
