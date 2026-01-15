package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.MediaModel
import java.util.Locale
import javax.inject.Inject

class MediaCacheOperations @Inject constructor(
    private val cache: MediaLibraryCache
) {
    fun getSiteImages(siteId: Int): List<MediaModel> {
        return filterByMimeType(siteId, "image")
    }

    fun getSiteVideos(siteId: Int): List<MediaModel> {
        return filterByMimeType(siteId, "video")
    }

    fun getSiteAudio(siteId: Int): List<MediaModel> {
        return filterByMimeType(siteId, "audio")
    }

    fun getSiteDocuments(siteId: Int): List<MediaModel> {
        return filterByMimeType(siteId, "application")
    }

    fun searchSiteImages(siteId: Int, searchTerm: String): List<MediaModel> {
        return searchByMimeTypeAndTerm(siteId, "image", searchTerm)
    }

    fun searchSiteVideos(siteId: Int, searchTerm: String): List<MediaModel> {
        return searchByMimeTypeAndTerm(siteId, "video", searchTerm)
    }

    fun searchSiteAudio(siteId: Int, searchTerm: String): List<MediaModel> {
        return searchByMimeTypeAndTerm(siteId, "audio", searchTerm)
    }

    fun searchSiteDocuments(siteId: Int, searchTerm: String): List<MediaModel> {
        return searchByMimeTypeAndTerm(siteId, "application", searchTerm)
    }

    fun getCacheSize(siteId: Int): Int {
        return cache.getMediaList(siteId)?.size ?: 0
    }

    fun getUploadedMediaCount(siteId: Int, mimeTypePrefix: String?): Int {
        if (mimeTypePrefix == null) {
            return getCacheSize(siteId)
        }
        return filterByMimeType(siteId, mimeTypePrefix).size
    }

    private fun filterByMimeType(siteId: Int, mimeTypePrefix: String): List<MediaModel> {
        val allMedia = cache.getMediaList(siteId) ?: return emptyList()
        return allMedia.filter { media ->
            media.mimeType?.startsWith(mimeTypePrefix) == true
        }
    }

    private fun searchByMimeTypeAndTerm(
        siteId: Int,
        mimeTypePrefix: String,
        searchTerm: String
    ): List<MediaModel> {
        val allMedia = cache.getMediaList(siteId) ?: return emptyList()
        val lowerSearchTerm = searchTerm.lowercase(Locale.ROOT)
        return allMedia.filter { media ->
            media.mimeType?.startsWith(mimeTypePrefix) == true &&
                    matchesSearchTerm(media, lowerSearchTerm)
        }
    }

    private fun matchesSearchTerm(media: MediaModel, lowerSearchTerm: String): Boolean {
        return (media.title?.contains(lowerSearchTerm, ignoreCase = true) == true) ||
            media.caption.contains(lowerSearchTerm, ignoreCase = true) ||
            media.description.contains(lowerSearchTerm, ignoreCase = true)
    }
}
