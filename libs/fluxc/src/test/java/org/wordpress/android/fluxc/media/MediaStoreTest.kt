package org.wordpress.android.fluxc.media

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.media.MediaTestUtils.generateMediaFromPath
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpapi.applicationpasswords.ApplicationPasswordsConfiguration
import org.wordpress.android.fluxc.network.rest.wpapi.media.ApplicationPasswordsMediaRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.media.wpv2.WPComV2MediaRestClient
import org.wordpress.android.fluxc.network.xmlrpc.media.MediaXMLRPCClient
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import org.wordpress.android.fluxc.store.MediaStore
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class MediaStoreTest {
    private lateinit var db: WPAndroidDatabase
    private lateinit var mediaStore: MediaStore
    private lateinit var testSite: SiteModel

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()

        mediaStore = MediaStore(
            Dispatcher(),
            mock<MediaXMLRPCClient>(),
            mock<WPComV2MediaRestClient>(),
            mock<ApplicationPasswordsMediaRestClient>(),
            mock<ApplicationPasswordsConfiguration>(),
            db.mediaDao()
        )

        testSite = SiteModel().apply { id = 1 }
    }

    private fun insertMedia(vararg media: MediaModel) {
        media.forEach { db.mediaDao().insert(it) }
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    fun `when getting site images, then only images are returned`() {
        val videoMedia = generateMediaFromPath(testSite.id, 1L, "/test/video.mp4")
        val imageMedia = generateMediaFromPath(testSite.id, 2L, "/test/image.jpg")
        insertMedia(videoMedia, imageMedia)

        val storeImages = mediaStore.getSiteImages(testSite)

        assertThat(storeImages)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactly(imageMedia)
    }

    @Test
    fun `when searching site images, then only matching images are returned`() {
        val imageMatching = generateMediaFromPath(testSite.id, 1L, "/test/image1.jpg")
            .copy(title = "test")
        val imageNotMatching = generateMediaFromPath(testSite.id, 2L, "/test/image2.jpg")
        val videoMatching = generateMediaFromPath(testSite.id, 3L, "/test/video.mp4")
            .copy(title = "test")

        insertMedia(imageMatching, imageNotMatching, videoMatching)

        val storeImages = mediaStore.searchSiteImages(testSite, "test")

        assertThat(storeImages)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactly(imageMatching)
    }

    @Test
    fun `when searching site videos, then only matching videos are returned`() {
        val videoMatching = generateMediaFromPath(testSite.id, 1L, "/test/video1.mp4")
            .copy(title = "test")
        val videoNotMatching = generateMediaFromPath(testSite.id, 2L, "/test/video2.mp4")
        val documentMatching = generateMediaFromPath(testSite.id, 3L, "/test/document.pdf")
            .copy(title = "test")

        insertMedia(videoMatching, videoNotMatching, documentMatching)

        val storeVideos = mediaStore.searchSiteVideos(testSite, "test")

        assertThat(storeVideos)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactly(videoMatching)
    }

    @Test
    fun `when searching site audio, then only matching audio files are returned`() {
        val audioMatching = generateMediaFromPath(testSite.id, 1L, "/test/audio1.mp3")
            .copy(title = "test")
        val audioNotMatching = generateMediaFromPath(testSite.id, 2L, "/test/audio2.mp3")
        val imageMatching = generateMediaFromPath(testSite.id, 3L, "/test/image.jpg")
            .copy(title = "test")

        insertMedia(audioMatching, audioNotMatching, imageMatching)

        val storeAudio = mediaStore.searchSiteAudio(testSite, "test")

        assertThat(storeAudio)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactly(audioMatching)
    }

    @Test
    fun `when searching site documents, then only matching documents are returned`() {
        val documentMatching = generateMediaFromPath(testSite.id, 1L, "/test/document1.pdf")
            .copy(title = "test")
        val documentNotMatching = generateMediaFromPath(testSite.id, 2L, "/test/document2.doc")
        val audioMatching = generateMediaFromPath(testSite.id, 3L, "/test/audio.mp3")
            .copy(title = "test")

        insertMedia(documentMatching, documentNotMatching, audioMatching)

        val storeDocuments = mediaStore.searchSiteDocuments(testSite, "test")

        assertThat(storeDocuments)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactly(documentMatching)
    }
}
