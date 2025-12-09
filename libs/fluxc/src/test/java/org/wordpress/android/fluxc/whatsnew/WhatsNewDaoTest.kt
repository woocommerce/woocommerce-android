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
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel.WhatsNewAnnouncementFeature
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.persistence.dao.WhatsNewDao
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity

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
    fun `announcements are stored and retrieved correctly`() = runBlocking {
        // Convert domain models to entities
        val announcementEntities = testAnnouncements.map { it.toEntity() }
        val featureEntities = testAnnouncements.flatMap { announcement ->
            announcement.features.map { feature ->
                feature.toEntity(announcement.announcementVersion)
            }
        }

        // Insert test data
        whatsNewDao.insertAnnouncements(announcementEntities)
        whatsNewDao.insertFeatures(featureEntities)

        // Retrieve and convert back to domain models
        val cachedAnnouncementEntities = whatsNewDao.getAllAnnouncements()
        val cachedAnnouncements = cachedAnnouncementEntities.map { announcementEntity ->
            val features = whatsNewDao.getFeaturesForAnnouncement(announcementEntity.announcementId.value)
            announcementEntity.toDomainModel(features)
        }

        assertEquals(testAnnouncements, cachedAnnouncements)
    }

    private fun WhatsNewAnnouncementModel.toEntity(): WhatsNewAnnouncementEntity {
        return WhatsNewAnnouncementEntity(
                announcementId = RemoteId(announcementVersion.toLong()),
                minimumAppVersion = minimumAppVersion,
                maximumAppVersion = maximumAppVersion,
                appVersionTargets = appVersionTargets,
                localized = isLocalized
        )
    }

    private fun WhatsNewAnnouncementFeature.toEntity(announcementVersion: Int): WhatsNewAnnouncementFeatureEntity {
        return WhatsNewAnnouncementFeatureEntity(
                announcementId = RemoteId(announcementVersion.toLong()),
                title = title ?: "",
                subtitle = subtitle,
                iconUrl = iconUrl,
                iconBase64 = iconBase64
        )
    }

    private fun WhatsNewAnnouncementEntity.toDomainModel(
            features: List<WhatsNewAnnouncementFeatureEntity>
    ): WhatsNewAnnouncementModel {
        return WhatsNewAnnouncementModel(
                announcementVersion = announcementId.value.toInt(),
                minimumAppVersion = minimumAppVersion,
                maximumAppVersion = maximumAppVersion,
                appVersionTargets = appVersionTargets,
                isLocalized = localized,
                features = features.map { it.toDomainModel() }
        )
    }

    private fun WhatsNewAnnouncementFeatureEntity.toDomainModel(): WhatsNewAnnouncementFeature {
        return WhatsNewAnnouncementFeature(
                title = title,
                subtitle = subtitle,
                iconBase64 = iconBase64,
                iconUrl = iconUrl
        )
    }
}
