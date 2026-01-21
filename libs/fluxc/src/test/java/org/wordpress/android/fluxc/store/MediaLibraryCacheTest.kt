package org.wordpress.android.fluxc.store

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.media.MediaTestUtils
import org.wordpress.android.fluxc.model.MediaModel
import java.util.concurrent.CountDownLatch

class MediaLibraryCacheTest {
    private lateinit var cache: MediaLibraryCache
    private lateinit var testMedia1: MediaModel
    private lateinit var testMedia2: MediaModel

    @Before
    fun setup() {
        cache = MediaLibraryCache()
        testMedia1 = createTestMedia(1, "image1.jpg")
        testMedia2 = createTestMedia(2, "image2.jpg")
    }

    @Test
    fun `when getting media list for uncached site, then it returns null`() {
        val result = cache.getMediaList(1)
        assertThat(result).isNull()
    }

    @Test
    fun `when caching media list, then it stores and retrieves correctly`() {
        val mediaList = listOf(testMedia1, testMedia2)

        cache.cacheMediaList(1, mediaList)
        val result = cache.getMediaList(1)

        assertThat(result).isEqualTo(mediaList)
    }

    @Test
    fun `when caching for different sites, then it stores different results`() {
        val list1 = listOf(testMedia1)
        val list2 = listOf(testMedia2)

        cache.cacheMediaList(1, list1)
        cache.cacheMediaList(2, list2)

        assertThat(cache.getMediaList(1)).isEqualTo(list1)
        assertThat(cache.getMediaList(2)).isEqualTo(list2)
    }

    @Test
    fun `when clearing, then it removes all entries`() {
        cache.cacheMediaList(1, listOf(testMedia1))
        cache.cacheMediaList(2, listOf(testMedia2))

        cache.clear()

        assertThat(cache.getMediaList(1)).isNull()
        assertThat(cache.getMediaList(2)).isNull()
    }

    @Test
    fun `when caching media list, then it overwrites previous value for same site`() {
        val list1 = listOf(testMedia1)
        val list2 = listOf(testMedia2)

        cache.cacheMediaList(1, list1)
        cache.cacheMediaList(1, list2)

        val result = cache.getMediaList(1)
        assertThat(result).isEqualTo(list2)
    }

    @Test
    fun `when caching empty list, then it stores correctly`() {
        cache.cacheMediaList(1, emptyList())

        val result = cache.getMediaList(1)
        assertThat(result).isEmpty()
    }

    @Test
    fun `when caching large list, then it stores correctly`() {
        val largeList = (1..100).map { createTestMedia(it, "image$it.jpg") }

        cache.cacheMediaList(1, largeList)
        val result = cache.getMediaList(1)

        assertThat(result).isEqualTo(largeList)
    }

    @Test
    fun `when adding new media, then it appends to list`() {
        val existingList = listOf(testMedia1)
        cache.cacheMediaList(1, existingList)

        cache.addOrUpdate(1, testMedia2)

        val result = cache.getMediaList(1)
        assertThat(result).containsExactly(testMedia1, testMedia2)
    }

    @Test
    fun `when updating existing media by media id, then it replaces media`() {
        val siteId = 123
        val original = createTestMedia(1, "original.jpg")
        val updated = createTestMedia(1, "updated.jpg")
        cache.cacheMediaList(siteId, listOf(original))

        cache.addOrUpdate(siteId, updated)

        val result = cache.getMediaList(siteId)
        assertThat(result).containsExactly(updated)
    }

    @Test
    fun `when adding to empty cache, then it creates new list`() {
        cache.addOrUpdate(1, testMedia1)

        val result = cache.getMediaList(1)
        assertThat(result).containsExactly(testMedia1)
    }

    @Test
    fun `when removing media by remote id, then it filters out media`() {
        val mediaToRemove = testMedia1
        cache.cacheMediaList(1, listOf(mediaToRemove, testMedia2))

        cache.remove(1, mediaToRemove.mediaId)

        val result = cache.getMediaList(1)
        assertThat(result).containsExactly(testMedia2)
    }

    @Test
    fun `when removing from empty cache, then it does nothing`() {
        cache.remove(1, 2)

        val result = cache.getMediaList(1)
        assertThat(result).isNull()
    }

    @Test
    fun `when multiple threads add media concurrently, then all updates are preserved`() {
        val siteId = 1
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val startLatch = CountDownLatch(1)

        val mediaItems = (1..threadCount).map { createTestMedia(it, "image$it.jpg") }

        // Launch threads that all try to add media at the same time
        mediaItems.map { media ->
            Thread {
                startLatch.await()
                cache.addOrUpdate(siteId, media)
                latch.countDown()
            }.apply { start() }
        }

        startLatch.countDown()
        latch.await()

        assertThat(cache.getMediaList(siteId))
            .hasSize(threadCount)
            .containsExactlyInAnyOrderElementsOf(mediaItems)
    }

    private fun createTestMedia(id: Int, fileName: String) =
        MediaTestUtils.createRemoteTestMedia().mediaId(id.toLong()).fileName(fileName).build()
}
