package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.LocalOrRemoteId.LocalId
import org.wordpress.android.fluxc.persistence.entity.SupportChatBookmarkEntity

@Dao
interface SupportChatBookmarkDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(bookmark: SupportChatBookmarkEntity): Long

    @Query("UPDATE SupportChatBookmarkEntity SET updatedAt = :updatedAt WHERE chatId = :chatId")
    suspend fun touch(chatId: Long, updatedAt: Long): Int

    @Query("SELECT * FROM SupportChatBookmarkEntity WHERE chatId = :chatId")
    suspend fun getByChatId(chatId: Long): SupportChatBookmarkEntity?

    @Query("SELECT * FROM SupportChatBookmarkEntity WHERE localSiteId = :localSiteId ORDER BY updatedAt DESC")
    suspend fun getForSite(localSiteId: LocalId): List<SupportChatBookmarkEntity>

    @Query("DELETE FROM SupportChatBookmarkEntity WHERE chatId = :chatId")
    suspend fun delete(chatId: Long): Int
}
