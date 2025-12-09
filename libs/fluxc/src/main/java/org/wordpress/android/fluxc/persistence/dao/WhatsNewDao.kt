package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementFeatureEntity
import org.wordpress.android.fluxc.persistence.entity.WhatsNewAnnouncementWithFeatures

@Dao
internal interface WhatsNewDao {
    @Transaction
    @Query("SELECT * FROM WhatsNewAnnouncementEntity")
    suspend fun getAnnouncementsWithFeatures(): List<WhatsNewAnnouncementWithFeatures>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<WhatsNewAnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFeatures(features: List<WhatsNewAnnouncementFeatureEntity>)

    @Query("DELETE FROM WhatsNewAnnouncementEntity")
    suspend fun deleteAllAnnouncements()
}
