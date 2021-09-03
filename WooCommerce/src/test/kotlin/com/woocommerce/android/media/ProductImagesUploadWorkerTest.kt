package com.woocommerce.android.media

import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent.UploadStarted
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateFailed
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateSucceeded
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUploadsCompleted
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.viewmodel.BaseUnitTest
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.util.DateTimeUtils
import java.util.*

@ExperimentalCoroutinesApi
class ProductImagesUploadWorkerTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private const val TEST_URI = "test"
        private val UPLOADED_MEDIA = MediaModel().apply {
            fileName = ""
            filePath = ""
            url = ""
            uploadDate = DateTimeUtils.iso8601FromDate(Date())
        }
    }

    private val notificationHandler: ProductImagesNotificationHandler = mock()
    private val productImagesServiceWrapper: ProductImagesServiceWrapper = mock()
    private lateinit var worker: ProductImagesUploadWorker
    private val mediaFilesRepository: MediaFilesRepository = mock {
        onBlocking { fetchMedia(TEST_URI) } doReturn MediaModel()
        onBlocking { uploadMedia(any(), any()) } doReturn UPLOADED_MEDIA
    }
    private val productDetailRepository: ProductDetailRepository = mock()
    private val resourceProvider: ResourceProvider = mock()


    @Before
    fun setup() {
        worker = ProductImagesUploadWorker(
            mediaFilesRepository = mediaFilesRepository,
            productDetailRepository = productDetailRepository,
            resourceProvider = resourceProvider,
            productImagesServiceWrapper = productImagesServiceWrapper,
            notificationHandler = notificationHandler,
            appCoroutineScope = TestCoroutineScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when there is pending work, then start service`() = testBlocking {
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        verify(productImagesServiceWrapper).startService()
    }

    @Test
    fun `when there is no pending work, then stop service`() = testBlocking {
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        advanceUntilIdle()
        verify(productImagesServiceWrapper).stopService()
    }

    @Test
    fun `when work is added before time expiration, then don't stop service`() = testBlocking {
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        advanceTimeBy(500L) // The worker waits 1 second before stopping the service

        // Enqueue more work
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        advanceTimeBy(500L) // To match the 1 second the worker will wait

        verify(productImagesServiceWrapper, never()).stopService()
    }

    @Test
    fun `when upload is enqueued, then start by fetching the media`() = testBlocking {
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        verify(mediaFilesRepository).fetchMedia(TEST_URI)
    }

    @Test
    fun `when media is fetched, then upload it`() = testBlocking {
        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        advanceUntilIdle()
        verify(mediaFilesRepository).uploadMedia(any(), any())
        assertThat(eventsList[0]).isEqualTo(UploadStarted(REMOTE_PRODUCT_ID, TEST_URI))
        assertThat(eventsList[1]).isEqualTo(UploadSucceeded(REMOTE_PRODUCT_ID, TEST_URI, UPLOADED_MEDIA))
        job.cancel()
    }

    @Test
    fun `when media upload finishes for a product, then send an event`() = testBlocking {
        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueImagesUpload(REMOTE_PRODUCT_ID, listOf(TEST_URI))

        advanceUntilIdle()
        assertThat(eventsList.last()).isEqualTo(ProductUploadsCompleted(REMOTE_PRODUCT_ID))
        job.cancel()
    }

    @Test
    fun `when update product is requested, then fetch product`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(product)

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        verify(productDetailRepository).fetchProduct(REMOTE_PRODUCT_ID)
    }

    @Test
    fun `when fetching product fails, then retry three times`() = testBlocking {
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(null)

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        verify(productDetailRepository, times(3)).fetchProduct(REMOTE_PRODUCT_ID)
    }

    @Test
    fun `when fetching product fails, then send an event`() = testBlocking {
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(null)

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateFailed(REMOTE_PRODUCT_ID))
        job.cancel()
    }

    @Test
    fun `when update product is requested, then update product`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(product)

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        verify(productDetailRepository)
            .updateProduct(product.copy(images = product.images + UPLOADED_MEDIA.toAppModel()))
    }

    @Test
    fun `when update product succeeds, then send an event`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(true)

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateSucceeded(REMOTE_PRODUCT_ID, product, 1))
        job.cancel()
    }

    @Test
    fun `when update product fails, then retry three times`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(false)

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        val updatedProduct = product.copy(images = product.images + UPLOADED_MEDIA.toAppModel())
        verify(productDetailRepository, times(3)).updateProduct(updatedProduct)
    }


    @Test
    fun `when update product fails, then send an event`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProduct(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(false)

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.addImagesToProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateFailed(REMOTE_PRODUCT_ID, product))
        job.cancel()
    }
}
