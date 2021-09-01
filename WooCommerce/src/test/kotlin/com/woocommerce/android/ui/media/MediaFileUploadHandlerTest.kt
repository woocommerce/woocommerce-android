package com.woocommerce.android.ui.media

import android.net.Uri
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploadFailed
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.media.MediaFileUploadHandler.ProductImageUploadData
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.FAILED
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState.UPLOADED
import org.wordpress.android.fluxc.store.MediaStore.MediaError
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType
import org.wordpress.android.fluxc.utils.DateUtils
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
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private lateinit var mediaFileUploadHandler: MediaFileUploadHandler

    @Before
    fun setup() {
        mediaFileUploadHandler = spy(
            MediaFileUploadHandler(
                resources,
                productImagesServiceWrapper,
                TestCoroutineScope(coroutinesTestRule.testDispatcher)
            )
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
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploaded(TEST_URI, mediaModel))
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
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploaded(TEST_URI, mediaModel))

        verify(productImagesServiceWrapper).addImagesToProduct(REMOTE_PRODUCT_ID, listOf(mediaModel.toAppModel()))
    }

    @Test
    fun `given there is no external observer, when multiple uploads finish, then start product update`() = testBlocking {
        val testUri2 = Uri.parse("file:///test2")
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI, testUri2))

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            fileName = "test"
            url = "url"
            uploadDate = DateTimeUtils.iso8601FromDate(Date())
            setUploadState(UPLOADED)
        }
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploaded(TEST_URI, mediaModel))
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploaded(testUri2, mediaModel))

        verify(productImagesServiceWrapper)
            .addImagesToProduct(REMOTE_PRODUCT_ID, listOf(mediaModel.toAppModel(), mediaModel.toAppModel()))
    }

    @Test
    fun `given there is external observer, when an upload fails, then skip handler's notification`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val job = launch { mediaFileUploadHandler.observeSuccessfulUploads(REMOTE_PRODUCT_ID).collect() }

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }
        val mediaError = MediaError(MediaErrorType.GENERIC_ERROR, "error")
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploadFailed(TEST_URI, mediaModel, mediaError))

        verify(productImagesServiceWrapper, never()).showUploadFailureNotification(any(), any())

        job.cancel()
    }

    @Test
    fun `given there is no external observer, when an upload fails, then show notification`() = testBlocking {
        mediaFileUploadHandler.enqueueUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        val mediaModel = MediaModel().apply {
            postId = REMOTE_PRODUCT_ID
            setUploadState(FAILED)
        }
        val mediaError = MediaError(MediaErrorType.GENERIC_ERROR, "error")
        mediaFileUploadHandler.onEventMainThread(OnProductImageUploadFailed(TEST_URI, mediaModel, mediaError))

        verify(productImagesServiceWrapper).showUploadFailureNotification(REMOTE_PRODUCT_ID, 1)
    }
}
