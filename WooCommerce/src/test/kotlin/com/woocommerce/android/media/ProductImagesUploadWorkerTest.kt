package com.woocommerce.android.media

import com.woocommerce.android.media.MediaFilesRepository.MediaUploadException
import com.woocommerce.android.media.MediaFilesRepository.UploadResult
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadProgress
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.UploadSuccess
import com.woocommerce.android.media.ProductImagesUploadWorker.Companion.DURATION_BEFORE_STOPPING_SERVICE
import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent.FetchSucceeded
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent.UploadFailed
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateFailed
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateSucceeded
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.ProductUploadsCompleted
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.ProductTestUtils
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.viewmodel.BaseUnitTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore.MediaErrorType.GENERIC_ERROR
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

@ExperimentalCoroutinesApi
class ProductImagesUploadWorkerTest : BaseUnitTest() {
    companion object {
        private const val REMOTE_PRODUCT_ID = 1L
        private const val TEST_URI = "test"
        private val FETCHED_MEDIA = MediaModel(0, 0)
        private val UPLOADED_MEDIA = MediaModel(0, 0).apply {
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
        onBlocking { fetchMedia(TEST_URI) } doReturn FETCHED_MEDIA
        onBlocking { uploadMedia(any(), any()) } doReturn flowOf(UploadResult.UploadSuccess(UPLOADED_MEDIA))
    }
    private val productDetailRepository: ProductDetailRepository = mock()

    @Before
    fun setup() {
        worker = ProductImagesUploadWorker(
            mediaFilesRepository = mediaFilesRepository,
            productDetailRepository = productDetailRepository,
            productImagesServiceWrapper = productImagesServiceWrapper,
            notificationHandler = notificationHandler,
            appCoroutineScope = TestScope(coroutinesTestRule.testDispatcher)
        )
    }

    @Test
    fun `when there is pending work, then start service`() = testBlocking {
        worker.enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, TEST_URI))

        verify(productImagesServiceWrapper).startService()
    }

    @Test
    fun `when there is no pending work, then stop service`() = testBlocking {
        worker.enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, TEST_URI))

        advanceUntilIdle()
        verify(productImagesServiceWrapper).stopService()
    }

    @Test
    fun `when work is added before time expiration, then don't stop service`() = testBlocking {
        worker.enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, TEST_URI))

        advanceTimeBy(DURATION_BEFORE_STOPPING_SERVICE / 2)

        // Enqueue more work
        worker.enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, "test uri 2"))

        advanceTimeBy(DURATION_BEFORE_STOPPING_SERVICE / 2)

        verify(productImagesServiceWrapper, never()).stopService()
    }

    @Test
    fun `when fetch media work is enqueued, then handle it`() = testBlocking {
        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueWork(Work.FetchMedia(REMOTE_PRODUCT_ID, TEST_URI))

        verify(mediaFilesRepository).fetchMedia(TEST_URI)
        assertThat(eventsList[0]).isEqualTo(FetchSucceeded(REMOTE_PRODUCT_ID, TEST_URI, FETCHED_MEDIA))
        job.cancel()
    }

    @Test
    fun `when media upload is requested, then handle it`() = testBlocking {
        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueWork(Work.UploadMedia(REMOTE_PRODUCT_ID, TEST_URI, MediaModel(0, 0)))

        advanceUntilIdle()
        verify(mediaFilesRepository).uploadMedia(any(), any())
        assertThat(eventsList[0]).isEqualTo(UploadSucceeded(REMOTE_PRODUCT_ID, TEST_URI, UPLOADED_MEDIA))
        job.cancel()
    }

    @Test
    fun `when media upload progress changes, then update notification`() = testBlocking {
        whenever(mediaFilesRepository.uploadMedia(any(), any()))
            .thenReturn(flowOf(UploadProgress(0.5f), UploadSuccess(MediaModel(0, 0))))

        worker.enqueueWork(Work.UploadMedia(REMOTE_PRODUCT_ID, TEST_URI, MediaModel(0, 0)))
        advanceUntilIdle()

        verify(notificationHandler).setProgress(0.5f)
    }

    @Test
    fun `when media upload fails for an image, then send an event`() = testBlocking {
        val error = MediaUploadException(
            errorType = GENERIC_ERROR,
            errorMessage = ""
        )
        whenever(mediaFilesRepository.uploadMedia(any(), any())).thenReturn(flowOf(UploadResult.UploadFailure(error)))

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueWork(Work.UploadMedia(REMOTE_PRODUCT_ID, TEST_URI, MediaModel(0, 0)))

        advanceUntilIdle()
        assertThat(eventsList).contains(UploadFailed(REMOTE_PRODUCT_ID, TEST_URI, error))
        job.cancel()
    }

    @Test
    fun `when media upload finishes for a product, then send an event`() = testBlocking {
        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }
        worker.enqueueWork(Work.UploadMedia(REMOTE_PRODUCT_ID, TEST_URI, MediaModel(0, 0)))

        advanceUntilIdle()
        assertThat(eventsList).contains(ProductUploadsCompleted(REMOTE_PRODUCT_ID))
        job.cancel()
    }

    @Test
    fun `when update product is requested, then fetch product`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(product)

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        verify(productDetailRepository).fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)
    }

    @Test
    fun `when fetching product fails, then retry three times`() = testBlocking {
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(null)

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        verify(productDetailRepository, times(ProductImagesUploadWorker.PRODUCT_UPDATE_RETRIES))
            .fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)
    }

    @Test
    fun `when fetching product fails, then send an event`() = testBlocking {
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(null)

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateFailed(REMOTE_PRODUCT_ID, null))
        job.cancel()
    }

    @Test
    fun `when update product is requested, then update product`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(product)

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        verify(productDetailRepository)
            .updateProduct(product.copy(images = product.images + UPLOADED_MEDIA.toAppModel()))
    }

    @Test
    fun `when update product succeeds, then send an event`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(Pair(true, null))

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateSucceeded(REMOTE_PRODUCT_ID, product, 1))
        job.cancel()
    }

    @Test
    fun `when update product fails, then retry three times`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(Pair(false, null))

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        val updatedProduct = product.copy(images = product.images + UPLOADED_MEDIA.toAppModel())
        verify(productDetailRepository, times(ProductImagesUploadWorker.PRODUCT_UPDATE_RETRIES))
            .updateProduct(updatedProduct)
    }

    @Test
    fun `when update product fails, then send an event`() = testBlocking {
        val product = ProductTestUtils.generateProduct(REMOTE_PRODUCT_ID)
        whenever(productDetailRepository.fetchProductOrLoadFromCache(REMOTE_PRODUCT_ID)).thenReturn(product)
        whenever(productDetailRepository.updateProduct(any())).thenReturn(Pair(false, null))

        val eventsList = mutableListOf<Event>()
        val job = launch {
            worker.events.toList(eventsList)
        }

        worker.enqueueWork(Work.UpdateProduct(REMOTE_PRODUCT_ID, listOf(UPLOADED_MEDIA)))

        assertThat(eventsList.last()).isEqualTo(ProductUpdateFailed(REMOTE_PRODUCT_ID, product))
        job.cancel()
    }
}
