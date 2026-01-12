package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.media.MediaTestUtils.generateMediaFromPath
import org.wordpress.android.fluxc.model.MediaModel
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class MediaDaoTest {
    private lateinit var db: WPAndroidDatabase

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().context
        db = Room.inMemoryDatabaseBuilder(
            context, WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()
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
    fun `when inserting media, then it can be retrieved with correct fields`() {
        val testMedia = generateMediaFromPath(1, 123L, "/test/test.jpg")
            .copy(title = "Test Title", caption = "Test Caption", description = "Test Description")

        insertMedia(testMedia)

        val retrieved = db.mediaDao().getSiteMediaWithId(1, 123L)

        assertThat(retrieved)
            .usingRecursiveComparison()
            .ignoringFields("id")
            .isEqualTo(testMedia)
    }

    @Test
    fun `when getting site images, then only images are returned`() {
        val image1 = generateMediaFromPath(1, 1L, "/test/image1.jpg")
        val image2 = generateMediaFromPath(1, 2L, "/test/image2.png")
        val video = generateMediaFromPath(1, 3L, "/test/video.mp4")

        insertMedia(image1, image2, video)

        val images = db.mediaDao().getSiteImages(1)

        assertThat(images)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(image1, image2)
    }

    @Test
    fun `when getting site videos, then only videos are returned`() {
        val image = generateMediaFromPath(1, 1L, "/test/image.jpg")
        val video1 = generateMediaFromPath(1, 2L, "/test/video1.mp4")
        val video2 = generateMediaFromPath(1, 3L, "/test/video2.mov")

        insertMedia(image, video1, video2)

        val videos = db.mediaDao().getSiteVideos(1)

        assertThat(videos)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(video1, video2)
    }

    @Test
    fun `when getting site audio, then only audio files are returned`() {
        val image = generateMediaFromPath(1, 1L, "/test/image.jpg")
        val audio1 = generateMediaFromPath(1, 2L, "/test/audio1.mp3")
        val audio2 = generateMediaFromPath(1, 3L, "/test/audio2.wav")

        insertMedia(image, audio1, audio2)

        val audio = db.mediaDao().getSiteAudio(1)

        assertThat(audio)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(audio1, audio2)
    }

    @Test
    fun `when getting site documents, then only documents are returned`() {
        val image = generateMediaFromPath(1, 1L, "/test/image.jpg")
        val video = generateMediaFromPath(1, 2L, "/test/video.mp4")
        val document1 = generateMediaFromPath(1, 3L, "/test/document.pdf")
        val document2 = generateMediaFromPath(1, 4L, "/test/spreadsheet.xls")

        insertMedia(image, video, document1, document2)

        val documents = db.mediaDao().getSiteDocuments(1)

        assertThat(documents)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(document1, document2)
    }

    @Test
    fun `when deleting media, then it is removed from database`() {
        val media = generateMediaFromPath(1, 1L, "/test/image.jpg")
        insertMedia(media)

        val retrieved = db.mediaDao().getSiteMediaWithId(1, 1L)
        db.mediaDao().deleteMedia(retrieved!!)

        val afterDelete = db.mediaDao().getSiteMediaWithId(1, 1L)
        assertThat(afterDelete).isNull()
    }

    @Test
    fun `when deleting uploaded media not in list, then only unlisted media is deleted`() {
        val uploaded1 = generateMediaFromPath(1, 1L, "/test/image1.jpg")
            .copy(uploadState = "UPLOADED")
        val uploaded2 = generateMediaFromPath(1, 2L, "/test/image2.jpg")
            .copy(uploadState = "UPLOADED")
        val uploaded3 = generateMediaFromPath(1, 3L, "/test/image3.jpg")
            .copy(uploadState = "UPLOADED")

        insertMedia(uploaded1, uploaded2, uploaded3)

        db.mediaDao().deleteUploadedSiteMediaNotInList(1, listOf(1L, 2L))

        val remaining = db.mediaDao().getSiteImages(1)
        assertThat(remaining)
            .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id")
            .containsExactlyInAnyOrder(uploaded1, uploaded2)
    }
}
