package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity

@Dao
internal interface WhatsNewDao {
    @Query("SELECT * FROM WhatsNewAnnouncementEntity")
    suspend fun getAllAnnouncements(): List<WhatsNewAnnouncementEntity>

    @Query("SELECT * FROM WhatsNewAnnouncementFeatureEntity WHERE announcementId = :announcementId")
    suspend fun getFeaturesForAnnouncement(announcementId: Long): List<WhatsNewAnnouncementFeatureEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<WhatsNewAnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatures(features: List<WhatsNewAnnouncementFeatureEntity>)

    @Query("DELETE FROM WhatsNewAnnouncementEntity")
    suspend fun deleteAllAnnouncements()
}
