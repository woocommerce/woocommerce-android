package org.wordpress.android.fluxc.whatsnew

import androidx.room.Room
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.persistence.WhatsNewMapper
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao

@RunWith(RobolectricTestRunner::class)
class WhatsNewDaoTest {
    private lateinit var database: WPAndroidDatabase
    private lateinit var whatsNewDao: WhatsNewDao

    private val firstAnnouncement = WhatsNewAnnouncementModel(
            announcementVersion = 1,
            minimumAppVersion = "14.7",
            maximumAppVersion = "14.9",
            appVersionTargets = emptyList(),
            isLocalized = true,
            features = listOf(
                    WhatsNewAnnouncementFeature(
                            title = "first announcement feature 1",
                            subtitle = "first announcement subtitle 1",
                            iconBase64 = "",
                            iconUrl = "https://wordpress.org/icon1.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            title = "first announcement feature 2",
                            subtitle = "first announcement subtitle 2",
                            iconBase64 = "<image data>",
                            iconUrl = ""
                    )
            )
    )

    private val secondAnnouncement = WhatsNewAnnouncementModel(
            announcementVersion = 2,
            minimumAppVersion = "14.9",
            maximumAppVersion = "16.0",
            appVersionTargets = listOf("alpha-111", "alpha-112"),
            isLocalized = false,
            features = listOf(
                    WhatsNewAnnouncementFeature(
                            title = "second announcement feature 1",
                            subtitle = "second announcement subtitle 1",
                            iconBase64 = "",
                            iconUrl = "https://wordpress.org/icon2.png"
                    ),
                    WhatsNewAnnouncementFeature(
                            title = "second announcement feature 2",
                            subtitle = "first announcement subtitle 2",
                            iconBase64 = "<second image data>",
                            iconUrl = ""
                    )
            )
    )

    private val testAnnouncements = listOf(firstAnnouncement, secondAnnouncement)

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext
        database = Room.inMemoryDatabaseBuilder(appContext, WPAndroidDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        whatsNewDao = database.whatsNewDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `when announcements are inserted, then they are retrieved correctly with features`() = runBlocking {
        // Convert domain models to entities
        val announcementsWithFeatures = testAnnouncements.map { WhatsNewMapper.fromDomainModel(it) }

        // Insert test data using single method
        whatsNewDao.insertAnnouncementsWithFeatures(announcementsWithFeatures)

        // Retrieve and convert back to domain models using Room's @Relation
        val cachedAnnouncementsWithFeatures = whatsNewDao.getAnnouncementsWithFeatures()
        val cachedAnnouncements = cachedAnnouncementsWithFeatures.map { WhatsNewMapper.toDomainModel(it) }

        assertEquals(testAnnouncements, cachedAnnouncements)
    }
}
