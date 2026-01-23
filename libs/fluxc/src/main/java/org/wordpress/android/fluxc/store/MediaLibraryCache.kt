package org.wordpress.android.fluxc.store

import org.wordpress.android.fluxc.model.MediaModel
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class MediaLibraryCache @Inject constructor() {
    private val cache = ConcurrentHashMap<Int, List<MediaModel>>()

    fun getMediaList(localSiteId: Int): List<MediaModel>? {
        return cache[localSiteId]
    }

    fun cacheMediaList(localSiteId: Int, mediaList: List<MediaModel>) {
        cache[localSiteId] = mediaList
    }

    fun addOrUpdate(localSiteId: Int, media: MediaModel) {
        cache.compute(localSiteId) { _, currentList ->
            val mutableList = (currentList ?: emptyList()).toMutableList()
            val existingIndex = mutableList.indexOfFirst { it.mediaId == media.mediaId }
            if (existingIndex != -1) {
                mutableList[existingIndex] = media
            } else {
                mutableList.add(media)
            }
            mutableList
        }
    }

    fun remove(localSiteId: Int, mediaId: Long) {
        cache.computeIfPresent(localSiteId) { _, currentList ->
            currentList.filter { it.mediaId != mediaId }
        }
    }

    fun clear() {
        cache.clear()
    }
}
