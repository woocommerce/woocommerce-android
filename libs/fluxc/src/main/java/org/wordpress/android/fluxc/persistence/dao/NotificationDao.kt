package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow
import org.wordpress.android.fluxc.model.LocalOrRemoteId.RemoteId
import org.wordpress.android.fluxc.persistence.entity.NotificationEntity

@Dao
internal abstract class NotificationDao {
    @Query("SELECT COUNT(*) FROM NotificationEntity")
    abstract suspend fun getNotificationsCount(): Int

    @Query("SELECT * FROM NotificationEntity ORDER BY timestamp DESC")
    abstract suspend fun getAllNotifications(): List<NotificationEntity>

    @Query(
        """
        SELECT * FROM NotificationEntity
        WHERE remoteSiteId = :remoteSiteId
        AND (
            (:filterByType IS NULL AND :filterBySubtype IS NULL)
            OR (:filterByType IS NOT NULL AND type IN (:filterByType))
            OR (:filterBySubtype IS NOT NULL AND subtype IN (:filterBySubtype))
        )
        ORDER BY timestamp DESC
        """
    )
    abstract suspend fun getNotificationsForSite(
        remoteSiteId: RemoteId,
        filterByType: List<String>?,
        filterBySubtype: List<String>?
    ): List<NotificationEntity>

    @Query(
        """
        SELECT * FROM NotificationEntity
        WHERE remoteSiteId = :remoteSiteId
        AND (
            (:filterByType IS NULL AND :filterBySubtype IS NULL)
            OR (:filterByType IS NOT NULL AND type IN (:filterByType))
            OR (:filterBySubtype IS NOT NULL AND subtype IN (:filterBySubtype))
        )
        ORDER BY timestamp DESC
        """
    )
    abstract fun observeNotificationsForSite(
        remoteSiteId: RemoteId,
        filterByType: List<String>?,
        filterBySubtype: List<String>?
    ): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1 FROM NotificationEntity
            WHERE remoteSiteId = :remoteSiteId
            AND read = 0
            AND (
                (:filterByType IS NULL AND :filterBySubtype IS NULL)
                OR (:filterByType IS NOT NULL AND type IN (:filterByType))
                OR (:filterBySubtype IS NOT NULL AND subtype IN (:filterBySubtype))
            )
        )
        """
    )
    abstract suspend fun hasUnreadNotificationsForSite(
        remoteSiteId: RemoteId,
        filterByType: List<String>?,
        filterBySubtype: List<String>?
    ): Boolean

    @Query("SELECT * FROM NotificationEntity WHERE remoteNoteId = :remoteNoteId LIMIT 1")
    abstract suspend fun getNotificationByRemoteId(remoteNoteId: RemoteId): NotificationEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insert(entity: NotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertAll(entities: List<NotificationEntity>)

    /**
     * Deletes the notifications matching the [remoteNoteIds] provided. This method uses
     * Kotlin's chunked functionality to ensure we don't crash with the "SQLiteException: too many SQL variables"
     * exception.
     */
    @Transaction
    open suspend fun deleteAllByRemoteIds(remoteNoteIds: List<RemoteId>) {
        remoteNoteIds.chunked(CHUNK_SIZE).forEach { chunk ->
            deleteAllByRemoteIdsForSubset(chunk)
        }
    }

    @Query("DELETE FROM NotificationEntity WHERE remoteNoteId IN (:remoteNoteIds)")
    protected abstract suspend fun deleteAllByRemoteIdsForSubset(remoteNoteIds: List<RemoteId>)

    private companion object {
        const val CHUNK_SIZE = 200
    }
}
