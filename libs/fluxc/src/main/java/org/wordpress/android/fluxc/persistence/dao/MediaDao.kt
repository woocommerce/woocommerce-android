package org.wordpress.android.fluxc.persistence.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import org.wordpress.android.fluxc.model.MediaModel

typealias MediaEntity = MediaModel

@Dao
internal abstract class MediaDao {
    @Query("SELECT * FROM MediaEntity WHERE localSiteId = :siteId AND mediaId = :mediaId ORDER BY uploadDate DESC LIMIT 1")
    abstract fun getSiteMediaWithId(siteId: Int, mediaId: Long): MediaEntity?

    @Query("SELECT * FROM MediaEntity WHERE localSiteId = :siteId AND uploadState IN (:uploadStates) ORDER BY uploadDate DESC")
    abstract fun getMediaWithStates(siteId: Int, uploadStates: List<String>): List<MediaEntity>

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType LIKE :mimeTypePrefix || '%'
        AND uploadState IN (:uploadStates)
        ORDER BY uploadDate DESC
    """)
    abstract fun getMediaWithStatesAndMimeType(
        siteId: Int,
        mimeTypePrefix: String,
        uploadStates: List<String>
    ): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE localSiteId = :siteId AND mimeType LIKE 'image/%' ORDER BY uploadDate DESC")
    abstract fun getSiteImages(siteId: Int): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE localSiteId = :siteId AND mimeType LIKE 'video/%' ORDER BY uploadDate DESC")
    abstract fun getSiteVideos(siteId: Int): List<MediaEntity>

    @Query("SELECT * FROM MediaEntity WHERE localSiteId = :siteId AND mimeType LIKE 'audio/%' ORDER BY uploadDate DESC")
    abstract fun getSiteAudio(siteId: Int): List<MediaEntity>

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType NOT LIKE 'image/%'
        AND mimeType NOT LIKE 'video/%'
        AND mimeType NOT LIKE 'audio/%'
        ORDER BY uploadDate DESC
    """)
    abstract fun getSiteDocuments(siteId: Int): List<MediaEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insert(media: MediaEntity)

    @Query("DELETE FROM MediaEntity WHERE localSiteId = :siteId AND uploadState = 'UPLOADED' AND mediaId NOT IN (:mediaIds)")
    abstract fun deleteUploadedSiteMediaNotInList(siteId: Int, mediaIds: List<Long>)

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType LIKE 'image/%'
        AND (title LIKE '%' || :searchTerm || '%'
             OR caption LIKE '%' || :searchTerm || '%'
             OR description LIKE '%' || :searchTerm || '%'
             OR mimeType LIKE '%' || :searchTerm || '%')
        ORDER BY uploadDate DESC
    """)
    abstract fun searchSiteImages(siteId: Int, searchTerm: String): List<MediaEntity>

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType LIKE 'video/%'
        AND (title LIKE '%' || :searchTerm || '%'
             OR caption LIKE '%' || :searchTerm || '%'
             OR description LIKE '%' || :searchTerm || '%'
             OR mimeType LIKE '%' || :searchTerm || '%')
        ORDER BY uploadDate DESC
    """)
    abstract fun searchSiteVideos(siteId: Int, searchTerm: String): List<MediaEntity>

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType LIKE 'audio/%'
        AND (title LIKE '%' || :searchTerm || '%'
             OR caption LIKE '%' || :searchTerm || '%'
             OR description LIKE '%' || :searchTerm || '%'
             OR mimeType LIKE '%' || :searchTerm || '%')
        ORDER BY uploadDate DESC
    """)
    abstract fun searchSiteAudio(siteId: Int, searchTerm: String): List<MediaEntity>

    @Query("""
        SELECT * FROM MediaEntity
        WHERE localSiteId = :siteId
        AND mimeType NOT LIKE 'image/%'
        AND mimeType NOT LIKE 'video/%'
        AND mimeType NOT LIKE 'audio/%'
        AND (title LIKE '%' || :searchTerm || '%'
             OR caption LIKE '%' || :searchTerm || '%'
             OR description LIKE '%' || :searchTerm || '%'
             OR mimeType LIKE '%' || :searchTerm || '%')
        ORDER BY uploadDate DESC
    """)
    abstract fun searchSiteDocuments(siteId: Int, searchTerm: String): List<MediaEntity>

    @Query("DELETE FROM MediaEntity WHERE id = :id AND mediaId = 0")
    protected abstract fun deleteLocalMediaById(id: Int)

    @Query("DELETE FROM MediaEntity WHERE localSiteId = :siteId AND (id = :id OR mediaId = :mediaId)")
    protected abstract fun deleteMediaBySiteAndIds(siteId: Int, id: Int, mediaId: Long)

    fun deleteMedia(media: MediaEntity) {
        if (media.mediaId == 0L) {
            deleteLocalMediaById(media.id)
        } else {
            deleteMediaBySiteAndIds(media.localSiteId, media.id, media.mediaId)
        }
    }
}
