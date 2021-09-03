package com.woocommerce.android.ui.media

import android.net.Uri
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesNotificationHandler
import com.woocommerce.android.media.ProductImagesUploadWorker
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.util.DateTimeUtils
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(RobolectricTestRunner::class)
class MediaFileUploadHandlerTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private val TEST_URI = Uri.parse("file:///test")
    }

    private val resources: ResourceProvider = mock()
    private val notificationHandler: ProductImagesNotificationHandler = mock()
    private val productImagesUploadWorker: ProductImagesUploadWorker = mock()
    private lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    private val eventsFlow = MutableSharedFlow<ProductImagesUploadWorker.Event>(extraBufferCapacity = Int.MAX_VALUE)

    @Before
    fun setup() {
        whenever(productImagesUploadWorker.events).thenReturn(eventsFlow)
        mediaFileUploadHandler = MediaFileUploadHandler(
            resourceProvider = resources,
            notificationHandler = notificationHandler,
            worker = productImagesUploadWorker,
            appCoroutineScope = TestCoroutineScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when an upload is requested, then update the status`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val ongoingUploads = mediaFileUploadHandler.observeCurrentUploads(REMOTE_PRODUCT_ID).first()

        assertThat(ongoingUploads).contains(TEST_URI)
    }

    @Test
    fun `given there is external observer, when upload finishes, then notify it`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        launch {
            val successfulUpload = mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).first()
            assertThat(successfulUpload.uploadState).isEqualTo(UPLOADED.toString())
        }

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(UPLOADED)
        }
        eventsFlow.tryEmit(
            ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                mediaModel
            )
        )
    }

    @Test
    fun `given there is no external observer, when uploads finish, then start product update`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            fileName = "test"
            url = "url"
            uploadDate = DateTimeUtils.iso8601FromDate(Date())
            setUploadState(UPLOADED)
        }
        eventsFlow.tryEmit(
            ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                mediaModel
            )
        )
        eventsFlow.tryEmit(ProductImagesUploadWorker.Event.ProductUploadsCompleted(REMOTE_PRODUCT_ID))
        verify(productImagesUploadWorker).addImagesToProduct(REMOTE_PRODUCT_ID, listOf(mediaModel))
    }

    @Test
    fun `given there is no external observer, when multiple uploads finish, then start product update`() =
        testBlocking {
            val testUri2 = Uri.parse("file:///test2")
            mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI, testUri2))

            val mediaModel = MediaModel().apply {
                postId = REMOTE_PRODUCT_ID
                fileName = "test"
                url = "url"
                uploadDate = DateTimeUtils.iso8601FromDate(Date())
                setUploadState(UPLOADED)
            }
            eventsFlow.tryEmit(
                ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded(
                    REMOTE_PRODUCT_ID,
                    TEST_URI,
                    mediaModel
                )
            )
            eventsFlow.tryEmit(
                ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded(
                    REMOTE_PRODUCT_ID,
                    testUri2,
                    mediaModel
                )
            )
            eventsFlow.tryEmit(ProductImagesUploadWorker.Event.ProductUploadsCompleted(REMOTE_PRODUCT_ID))

            verify(productImagesUploadWorker).addImagesToProduct(REMOTE_PRODUCT_ID, listOf(mediaModel, mediaModel))
        }

    @Test
    fun `given there is external observer, when an upload fails, then skip handler's notification`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val job = launch { mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).collect() }

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }

        eventsFlow.tryEmit(
            ProductImagesUploadWorker.Event.MediaUploadEvent.UploadFailed(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                MediaFilesRepository.MediaUploadException(
                    media = mediaModel,
                    errorMessage = "error",
                    errorType = MediaErrorType.GENERIC_ERROR
                )
            )
        )

        verify(notificationHandler, never()).postUploadFailureNotification(any(), any())

        job.cancel()
    }

    @Test
    fun `given there is no external observer, when an upload fails, then show notification`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }
        eventsFlow.tryEmit(
            ProductImagesUploadWorker.Event.MediaUploadEvent.UploadFailed(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                MediaFilesRepository.MediaUploadException(
                    media = mediaModel,
                    errorMessage = "error",
                    errorType = MediaErrorType.GENERIC_ERROR
                )
            )
        )

        verify(notificationHandler).postUploadFailureNotification(REMOTE_PRODUCT_ID, 1)
    }
}
