package org.wordpress.android.fluxc.whatsnew

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.WPDatabaseTestRule
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.persistence.entity.AppVersionTargets
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementWithFeatures

@RunWith(RobolectricTestRunner::class)
class WhatsNewDaoTest {
    @Rule
    @JvmField
    val wpDatabaseRule = WPDatabaseTestRule(
        ApplicationProvider.getApplicationContext()
    )

    private lateinit var whatsNewDao: WhatsNewDao

    private val firstAnnouncementEntity = WhatsNewAnnouncementEntity(
        announcementId = RemoteId(1),
        minimumAppVersion = "14.7",
        maximumAppVersion = "14.9",
        appVersionTargets = AppVersionTargets(emptyList()),
        localized = true
    )

    private val firstAnnouncementFeatures = listOf(
        WhatsNewAnnouncementFeatureEntity(
            announcementId = RemoteId(1),
            title = "first announcement feature 1",
            subtitle = "first announcement subtitle 1",
            iconBase64 = "",
            iconUrl = "https://wordpress.org/icon1.png"
        ),
        WhatsNewAnnouncementFeatureEntity(
            announcementId = RemoteId(1),
            title = "first announcement feature 2",
            subtitle = "first announcement subtitle 2",
            iconBase64 = "<image data>",
            iconUrl = ""
        )
    )

    private val secondAnnouncementEntity = WhatsNewAnnouncementEntity(
        announcementId = RemoteId(2),
        minimumAppVersion = "14.9",
        maximumAppVersion = "16.0",
        appVersionTargets = AppVersionTargets(listOf("alpha-111", "alpha-112")),
        localized = false
    )

    private val secondAnnouncementFeatures = listOf(
        WhatsNewAnnouncementFeatureEntity(
            announcementId = RemoteId(2),
            title = "second announcement feature 1",
            subtitle = "second announcement subtitle 1",
            iconBase64 = "",
            iconUrl = "https://wordpress.org/icon2.png"
        ),
        WhatsNewAnnouncementFeatureEntity(
            announcementId = RemoteId(2),
            title = "second announcement feature 2",
            subtitle = "second announcement subtitle 2",
            iconBase64 = "<second image data>",
            iconUrl = ""
        )
    )

    private val testAnnouncementsWithFeatures = listOf(
        WhatsNewAnnouncementWithFeatures(
            announcement = firstAnnouncementEntity,
            features = firstAnnouncementFeatures
        ),
        WhatsNewAnnouncementWithFeatures(
            announcement = secondAnnouncementEntity,
            features = secondAnnouncementFeatures
        )
    )

    @Before
    fun setUp() {
        whatsNewDao = wpDatabaseRule.db.whatsNewDao()
    }

    @Test
    fun `when announcements are replaced, then they are retrieved correctly with features`() = runBlocking {
        whatsNewDao.replaceAnnouncements(testAnnouncementsWithFeatures)

        val result = whatsNewDao.getAnnouncementsWithFeatures()

        assertEquals(testAnnouncementsWithFeatures, result)
    }

    @Test
    fun `when announcements are replaced twice, then old announcements and features are deleted`(): Unit = runBlocking {
        whatsNewDao.replaceAnnouncements(testAnnouncementsWithFeatures)
        val newAnnouncement = WhatsNewAnnouncementWithFeatures(
            announcement = WhatsNewAnnouncementEntity(
                announcementId = RemoteId(3),
                minimumAppVersion = "16.0",
                maximumAppVersion = "17.0",
                appVersionTargets = AppVersionTargets(emptyList()),
                localized = true
            ),
            features = listOf(
                WhatsNewAnnouncementFeatureEntity(
                    announcementId = RemoteId(3),
                    title = "new feature",
                    subtitle = "new subtitle",
                    iconBase64 = "",
                    iconUrl = ""
                )
            )
        )

        whatsNewDao.replaceAnnouncements(listOf(newAnnouncement))
        val result = whatsNewDao.getAnnouncementsWithFeatures()

        assertThat(result).hasSize(1).first().matches {
            it.announcement.announcementId == RemoteId(3)
        }
    }

    @Test
    fun `when appVersionTargets contains commas, then they are preserved using semicolon separator`(): Unit =
        runBlocking {
            val announcementWithCommas = WhatsNewAnnouncementWithFeatures(
                announcement = WhatsNewAnnouncementEntity(
                    announcementId = RemoteId(999),
                    minimumAppVersion = "1.0",
                    maximumAppVersion = "2.0",
                    appVersionTargets = AppVersionTargets(listOf("alpha-1,2", "beta-3,4,5")),
                    localized = false
                ),
                features = emptyList()
            )

            whatsNewDao.replaceAnnouncements(listOf(announcementWithCommas))
            val result = whatsNewDao.getAnnouncementsWithFeatures()

            assertThat(result).hasSize(1).first().matches {
                it.announcement.appVersionTargets == AppVersionTargets(listOf("alpha-1,2", "beta-3,4,5"))
            }
        }
}
