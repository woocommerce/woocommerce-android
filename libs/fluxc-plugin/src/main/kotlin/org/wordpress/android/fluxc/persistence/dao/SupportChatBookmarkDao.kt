package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity

@Dao
interface SupportChatBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(bookmark: SupportChatBookmarkEntity)

    @Query(
        """
        UPDATE SupportChatBookmarkEntity
        SET updatedAt = :updatedAt,
            sessionId = CASE WHEN :sessionId IS NULL THEN sessionId ELSE :sessionId END
        WHERE chatId = :chatId
        """
    )
    suspend fun markAsUpdated(chatId: Long, sessionId: String?, updatedAt: Long): Int

    @Query("SELECT * FROM SupportChatBookmarkEntity WHERE chatId = :chatId")
    suspend fun getByChatId(chatId: Long): SupportChatBookmarkEntity?

    @Query("SELECT * FROM SupportChatBookmarkEntity WHERE localSiteId = :localSiteId ORDER BY updatedAt DESC")
    suspend fun getForSite(localSiteId: LocalId): List<SupportChatBookmarkEntity>

    @Query("UPDATE SupportChatBookmarkEntity SET hasCreatedTicket = 1 WHERE chatId = :chatId")
    suspend fun markTicketCreated(chatId: Long): Int

    @Query("UPDATE SupportChatBookmarkEntity SET isResolved = 1 WHERE chatId = :chatId")
    suspend fun markResolved(chatId: Long): Int

    @Query("DELETE FROM SupportChatBookmarkEntity WHERE chatId = :chatId")
    suspend fun delete(chatId: Long): Int
}
