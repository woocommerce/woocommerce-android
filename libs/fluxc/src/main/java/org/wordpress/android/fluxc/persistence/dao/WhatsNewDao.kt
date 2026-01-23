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
internal abstract class WhatsNewDao {
    @Transaction
    @Query("SELECT * FROM WhatsNewAnnouncementEntity")
    abstract suspend fun getAnnouncementsWithFeatures(): List<WhatsNewAnnouncementWithFeatures>

    @Transaction
    open suspend fun replaceAnnouncements(
        announcementsWithFeatures: List<WhatsNewAnnouncementWithFeatures>
    ) {
        deleteAllAnnouncements()
        insertAnnouncementsWithFeatures(announcementsWithFeatures)
    }

    @Transaction
    protected open suspend fun insertAnnouncementsWithFeatures(
        announcementsWithFeatures: List<WhatsNewAnnouncementWithFeatures>
    ) {
        val announcements = announcementsWithFeatures.map { it.announcement }
        val features = announcementsWithFeatures.flatMap { it.features }
        insertAnnouncements(announcements)
        insertFeatures(features)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertAnnouncements(announcements: List<WhatsNewAnnouncementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    protected abstract suspend fun insertFeatures(features: List<WhatsNewAnnouncementFeatureEntity>)

    @Query("DELETE FROM WhatsNewAnnouncementEntity")
    protected abstract suspend fun deleteAllAnnouncements()
}
