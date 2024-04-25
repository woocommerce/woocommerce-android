package com.woocommerce.android.ui.media

import com.woocommerce.android.R.string
import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.media.ProductImagesNotificationHandler
import com.woocommerce.android.media.ProductImagesUploadWorker
import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.NULL_MEDIA_ARG
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@ExperimentalCoroutinesApi
class MediaFileUploadHandlerTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private const val TEST_URI = "test"
    }

    private val eventsFlow = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)

    private val notificationHandler: ProductImagesNotificationHandler = mock()
    private val productImagesUploadWorker: ProductImagesUploadWorker = mock {
        on { events } doReturn eventsFlow
    }
    private val resourceProvider: ResourceProvider = mock {
        on { getString(any()) } doAnswer { it.arguments.first().toString() }
    }
    private val productDetailRepository: ProductDetailRepository = mock()
    private lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    @Before
    fun setup() {
        mediaFileUploadHandler = MediaFileUploadHandler(
            notificationHandler = notificationHandler,
            worker = productImagesUploadWorker,
            resourceProvider = resourceProvider,
            productDetailRepository = productDetailRepository,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when an upload is requested, then update the status`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val ongoingUploads = mediaFileUploadHandler.observeCurrentUploads(REMOTE_PRODUCT_ID).first()

        assertThat(ongoingUploads).contains(TEST_URI)
    }

    @Test
    fun `when media is fetched, then start uploading it`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val fetchedMedia = MediaModel(0, 0)
        eventsFlow.tryEmit(
            Event.MediaUploadEvent.FetchSucceeded(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                fetchedMedia
            )
        )

        verify(productImagesUploadWorker).enqueueWork(
            Work.UploadMedia(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                fetchedMedia
            )
        )
    }

    @Test
    fun `given there is no external observer, when fetching media fails, then notify failure`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        eventsFlow.tryEmit(
            Event.MediaUploadEvent.FetchFailed(
                REMOTE_PRODUCT_ID,
                TEST_URI
            )
        )

        val error = ProductImageUploadData(
            remoteProductId = REMOTE_PRODUCT_ID,
            localUri = TEST_URI,
            uploadStatus = UploadStatus.Failed(
                mediaErrorMessage = resourceProvider.getString(string.product_image_service_error_media_null),
                mediaErrorType = NULL_MEDIA_ARG
            )
        )
        verify(notificationHandler).postUploadFailureNotification(anyOrNull(), eq(listOf(error)))
    }

    @Test
    fun `given there is external observer, when upload finishes, then notify it`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        launch {
            val successfulUpload = mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).first()
            assertThat(successfulUpload.uploadState).isEqualTo(UPLOADED.toString())
        }

        val mediaModel = MediaModel(0, 0).apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(UPLOADED)
        }
        eventsFlow.tryEmit(
            Event.MediaUploadEvent.UploadSucceeded(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                mediaModel
            )
        )
    }

    @Test
    fun `given there is no external observer, when uploads finish, then start product update`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val mediaModel = MediaModel(0, 0).apply {
            postId = REMOTE_PRODUCT_ID
            fileName = "test"
            url = "url"
            uploadDate = DateTimeUtils.iso8601FromDate(Date())
            setUploadState(UPLOADED)
        }
        eventsFlow.tryEmit(
            Event.MediaUploadEvent.UploadSucceeded(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                mediaModel
            )
        )
        eventsFlow.tryEmit(Event.ProductUploadsCompleted(REMOTE_PRODUCT_ID))
        verify(productImagesUploadWorker).enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(mediaModel)))
    }

    @Test
    fun `given there is no external observer, when multiple uploads finish, then start product update`() =
        testBlocking {
            val testUri2 = "file:///test2"
            mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI, testUri2))

            val mediaModel = MediaModel(0, 0).apply {
                postId = REMOTE_PRODUCT_ID
                fileName = "test"
                url = "url"
                uploadDate = DateTimeUtils.iso8601FromDate(Date())
                setUploadState(UPLOADED)
            }
            eventsFlow.tryEmit(
                Event.MediaUploadEvent.UploadSucceeded(
                    REMOTE_PRODUCT_ID,
                    TEST_URI,
                    mediaModel
                )
            )
            eventsFlow.tryEmit(
                Event.MediaUploadEvent.UploadSucceeded(
                    REMOTE_PRODUCT_ID,
                    testUri2,
                    mediaModel
                )
            )
            eventsFlow.tryEmit(Event.ProductUploadsCompleted(REMOTE_PRODUCT_ID))

            verify(productImagesUploadWorker)
                .enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(mediaModel, mediaModel)))
        }

    @Test
    fun `given there is external observer, when an upload fails, then skip handler's notification`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val job = launch { mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).collect() }

        val mediaModel = MediaModel(0, 0).apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }

        eventsFlow.tryEmit(
            Event.MediaUploadEvent.UploadFailed(
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

        val mediaModel = MediaModel(0, 0).apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }
        eventsFlow.tryEmit(
            Event.MediaUploadEvent.UploadFailed(
                REMOTE_PRODUCT_ID,
                TEST_URI,
                MediaFilesRepository.MediaUploadException(
                    media = mediaModel,
                    errorMessage = "error",
                    errorType = MediaErrorType.GENERIC_ERROR
                )
            )
        )

        val errorData = ProductImageUploadData(
            remoteProductId = REMOTE_PRODUCT_ID,
            localUri = TEST_URI,
            uploadStatus = UploadStatus.Failed(
                media = mediaModel,
                mediaErrorMessage = "error",
                mediaErrorType = MediaErrorType.GENERIC_ERROR
            )
        )

        verify(notificationHandler).postUploadFailureNotification(anyOrNull(), eq(listOf(errorData)))
    }

    @Test
    fun `when assigning uploads to created product, then update the id for the successful ones`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID, listOf(TEST_URI))
        val mediaModel = MediaModel(0, 0).apply {
            fileName = "test"
            url = "url"
            uploadDate = DateTimeUtils.iso8601FromDate(Date())
            setUploadState(UPLOADED)
        }
        eventsFlow.tryEmit(
            Event.MediaUploadEvent.UploadSucceeded(
                ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID,
                TEST_URI,
                mediaModel
            )
        )

        mediaFileUploadHandler.assignUploadsToCreatedProduct(REMOTE_PRODUCT_ID)

        val uploadedMedia = mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).first()
        assertThat(uploadedMedia).isEqualTo(mediaModel)
    }

    @Test
    fun `when assigning uploads to created product, then cancel and reschedule ongoing ones`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID, listOf(TEST_URI))

        mediaFileUploadHandler.assignUploadsToCreatedProduct(REMOTE_PRODUCT_ID)

        verify(productImagesUploadWorker).cancelUpload(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
        verify(productImagesUploadWorker).enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, TEST_URI))
        val currentUploads = mediaFileUploadHandler.observeCurrentUploads(REMOTE_PRODUCT_ID).first()
        assertThat(currentUploads).isEqualTo(listOf(TEST_URI))
    }
}
