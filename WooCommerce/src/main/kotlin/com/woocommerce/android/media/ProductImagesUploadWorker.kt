package com.woocommerce.android.media

import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.media.MediaFilesRepository.UploadResult.*
import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Event.MediaUploadEvent
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.MediaModel
import javax.inject.Inject
import javax.inject.Singleton

/***
 * This class is responsible for queuing and handling different tasks related to product images upload.
 * It handles three types of works:
 * - [Work.FetchMedia]
 * - [Work.UploadMedia]
 * - [Work.UpdateProduct]
 *
 * And notifies the consumers using the [Event]s it publishes.
 *
 * Uploading media and updating product is done sequentially (using a [Mutex] lock) because our repositories don't
 * play well with parallel requests, due to the use of a single shared continuation.
 */
@Singleton
class ProductImagesUploadWorker @Inject constructor(
    private val mediaFilesRepository: MediaFilesRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val notificationHandler: ProductImagesNotificationHandler,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    companion object {
        const val DURATION_BEFORE_STOPPING_SERVICE = 1000L
        const val PRODUCT_UPDATE_RETRIES = 3
    }

    private val queue = MutableSharedFlow<Work>(extraBufferCapacity = Int.MAX_VALUE)
    private val pendingWorkList = MutableStateFlow<List<Work>>(emptyList())

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)
    val events = _events.asSharedFlow()

    private val cancelledProducts = mutableSetOf<Long>()
    private val currentJobs = mutableMapOf<Long, List<Job>>()

    // A reference to all images being uploaded to update the notification with the correct index
    private val uploadList = mutableListOf<MediaUploadEntry>()

    private val mutex = Mutex()

    init {
        observeQueue()
        handleServiceStatus()
        updateUploadList()
    }

    private fun observeQueue() {
        queue
            .onEach { work ->
                pendingWorkList.update { list -> list + work }
                if (work is Work.FetchMedia) {
                    uploadList.add(MediaUploadEntry(work.productId, work.localUri))
                }
                handleWork(work)
            }
            .launchIn(appCoroutineScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun handleServiceStatus() {
        pendingWorkList
            .transformLatest { list ->
                val done = list.isEmpty()
                if (done) {
                    // Add a delay to avoid stopping the service if there is an event coming to the queue
                    delay(DURATION_BEFORE_STOPPING_SERVICE)
                }
                emit(done)
            }
            .distinctUntilChanged()
            .onEach { done ->
                if (done) {
                    WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> stop service")
                    productImagesServiceWrapper.stopService()
                    uploadList.clear()
                    emitEvent(Event.ServiceStopped)
                } else {
                    WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start service")
                    productImagesServiceWrapper.startService()
                }
            }
            .launchIn(appCoroutineScope)
    }

    private fun updateUploadList() {
        events
            .filterIsInstance<MediaUploadEvent>()
            .filter {
                val isUploadCompleted = it is MediaUploadEvent.FetchFailed ||
                    it is MediaUploadEvent.UploadSucceeded ||
                    it is MediaUploadEvent.UploadFailed
                isUploadCompleted
            }
            .onEach { event ->
                val index =
                    uploadList.indexOfFirst { it.productId == event.productId && it.localUri == event.localUri }
                uploadList[index] = uploadList[index].copy(isDone = true)
            }
            .launchIn(appCoroutineScope)
    }

    /**
     * Allows filtering events for cancelled operations.
     * Please use this instead of accessing [_events] directly
     */
    private suspend fun emitEvent(event: Event) {
        if (cancelledProducts.contains(event.productId)) return
        _events.emit(event)
    }

    private fun handleWork(work: Work) {
        if (cancelledProducts.contains(work.productId)) {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> skipping work $work since it's cancelled")
            return
        }

        val job = appCoroutineScope.launch {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start work handling $work")

            try {
                when (work) {
                    is Work.FetchMedia -> fetchMedia(work)
                    is Work.UploadMedia -> uploadMedia(work)
                    is Work.UpdateProduct -> updateProduct(work)
                }
            } finally {
                pendingWorkList.update { list -> list - work }
            }
        }

        // Save a reference to the job for cancelling it if needed
        currentJobs[work.productId] = currentJobs.getOrElse(work.productId) { emptyList() } + job

        job.invokeOnCompletion {
            // Remove the job from the list jobs
            currentJobs[work.productId] = currentJobs[work.productId]!! - job
        }
    }

    fun enqueueWork(work: Work) {
        cancelledProducts.remove(work.productId)
        queue.tryEmit(work)
    }

    fun cancelUpload(productId: Long) {
        cancelledProducts.add(productId)
        currentJobs[productId]?.forEach {
            it.cancel()
        }
        uploadList.removeAll { it.productId == productId }
    }

    private suspend fun fetchMedia(work: Work.FetchMedia) {
        WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> fetch media ${work.localUri}")

        val fetchedMedia = mediaFilesRepository.fetchMedia(work.localUri)
        if (fetchedMedia == null) {
            WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> fetching media failed")

            emitEvent(
                MediaUploadEvent.FetchFailed(
                    productId = work.productId,
                    localUri = work.localUri
                )
            )
        } else {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> media fetched, enqueue upload")
            emitEvent(
                MediaUploadEvent.FetchSucceeded(
                    productId = work.productId,
                    localUri = work.localUri,
                    fetchedMedia = fetchedMedia
                )
            )
        }
    }

    private suspend fun uploadMedia(work: Work.UploadMedia) {
        mutex.withLock {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start uploading media ${work.localUri}")

            val doneUploads = uploadList.count { it.isDone }
            notificationHandler.update(doneUploads + 1, uploadList.size)
            work.fetchedMedia.postId = work.productId
            mediaFilesRepository.uploadMedia(work.fetchedMedia).collect {
                when (it) {
                    is UploadFailure -> {
                        WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> upload failed for ${work.localUri}")
                        emitEvent(
                            MediaUploadEvent.UploadFailed(
                                productId = work.productId,
                                localUri = work.localUri,
                                error = it.error
                            )
                        )
                    }
                    is UploadProgress -> notificationHandler.setProgress(it.progress)
                    is UploadSuccess -> {
                        WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> upload succeeded for ${work.localUri}")
                        notificationHandler.setProgress(1f)
                        emitEvent(
                            MediaUploadEvent.UploadSucceeded(
                                productId = work.productId,
                                localUri = work.localUri,
                                media = it.media
                            )
                        )
                    }
                }
            }

            val hasMoreUploads = pendingWorkList.value.any {
                it != work && it.productId == work.productId && (it is Work.UploadMedia || it is Work.FetchMedia)
            }
            if (!hasMoreUploads) {
                WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> all uploads for product ${work.productId} are done")
                emitEvent(Event.ProductUploadsCompleted(work.productId))
            }
        }
    }

    private suspend fun updateProduct(work: Work.UpdateProduct) {
        suspend fun fetchProductWithRetries(productId: Long): Product? {
            var retries = 0
            while (retries < PRODUCT_UPDATE_RETRIES) {
                val product = productDetailRepository.fetchProductOrLoadFromCache(productId)
                if (product != null && productDetailRepository.lastFetchProductErrorType == null) {
                    return product
                }
                retries++
            }
            return null
        }

        suspend fun updateProductWithRetries(product: Product): Boolean {
            var retries = 0
            while (retries < PRODUCT_UPDATE_RETRIES) {
                val result = productDetailRepository.updateProduct(product)
                if (result.first) {
                    return true
                }
                retries++
            }
            return false
        }
        mutex.withLock {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start updating product ${work.productId}")

            val images = work.addedImages.map { it.toAppModel() }

            val cachedProduct = productDetailRepository.getProduct(work.productId)

            notificationHandler.showUpdatingProductNotification(cachedProduct)

            val product = fetchProductWithRetries(work.productId)
            if (product == null) {
                WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> fetching product ${work.productId} failed")
                emitEvent(Event.ProductUpdateEvent.ProductUpdateFailed(work.productId, cachedProduct))
                return@withLock
            }

            notificationHandler.showUpdatingProductNotification(product)
            val success = updateProductWithRetries(product.copy(images = product.images + images))
            if (success) {
                WooLog.d(
                    T.MEDIA,
                    "ProductImagesUploadWorker -> added ${images.size} images to product ${work.productId}"
                )
                emitEvent(Event.ProductUpdateEvent.ProductUpdateSucceeded(work.productId, product, images.size))
            } else {
                WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> updating product ${work.productId} failed")
                emitEvent(Event.ProductUpdateEvent.ProductUpdateFailed(work.productId, product))
            }
        }
    }

    sealed class Work {
        abstract val productId: Long

        data class FetchMedia(
            override val productId: Long,
            val localUri: String
        ) : Work()

        data class UploadMedia(
            override val productId: Long,
            val localUri: String,
            val fetchedMedia: MediaModel,
        ) : Work()

        data class UpdateProduct(
            override val productId: Long,
            val addedImages: List<MediaModel>
        ) : Work()
    }

    sealed class Event {
        abstract val productId: Long

        data class ProductUploadsCompleted(
            override val productId: Long
        ) : Event()

        object ServiceStopped : Event() {
            // This event concerns whole worker, not a single product
            override val productId: Long = 0L
        }

        sealed class MediaUploadEvent : Event() {
            abstract val localUri: String

            data class FetchSucceeded(
                override val productId: Long,
                override val localUri: String,
                val fetchedMedia: MediaModel
            ) : MediaUploadEvent()

            data class FetchFailed(
                override val productId: Long,
                override val localUri: String
            ) : MediaUploadEvent()

            data class UploadSucceeded(
                override val productId: Long,
                override val localUri: String,
                val media: MediaModel
            ) : MediaUploadEvent()

            data class UploadFailed(
                override val productId: Long,
                override val localUri: String,
                val error: MediaFilesRepository.MediaUploadException
            ) : MediaUploadEvent()
        }

        sealed class ProductUpdateEvent : Event() {
            data class ProductUpdateSucceeded(
                override val productId: Long,
                val product: Product,
                val imagesCount: Int
            ) : ProductUpdateEvent()

            data class ProductUpdateFailed(
                override val productId: Long,
                val product: Product?
            ) : ProductUpdateEvent()
        }
    }

    /**
     * This class is only used to be able to count total of uploads, and number of completed ones, and update
     * notification accordingly
     */
    data class MediaUploadEntry(
        val productId: Long,
        val localUri: String,
        val isDone: Boolean = false
    )
}
