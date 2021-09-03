package com.woocommerce.android.media

import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.update
import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooLog.T
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
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
    private val resourceProvider: ResourceProvider,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    private val notificationHandler: ProductImagesNotificationHandler,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    private val queue = MutableSharedFlow<Work>(extraBufferCapacity = Int.MAX_VALUE)
    private val currentWorkListCount = MutableStateFlow<List<Work>>(emptyList())

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = Int.MAX_VALUE)
    val events = _events.asSharedFlow()

    private val cancelledProducts = mutableSetOf<Long>()
    private val currentJobs = mutableMapOf<Long, List<Job>>()

    // A reference to all images being uploaded to update the notification with the correct index
    private val listOfImagesToUpload = mutableListOf<Pair<Long, String>>()

    private val mutex = Mutex()

    init {
        queue
            .onEach { work ->
                currentWorkListCount.update { list -> list + work }
                handleWork(work)
            }
            .launchIn(appCoroutineScope)

        currentWorkListCount
            .transformLatest { list ->
                println("images -> worklist changes $list")
                val done = list.isEmpty()
                if (done) {
                    // Add a delay to avoid stopping the service if there is an event coming to the queue
                    delay(1000L)
                }
                emit(done)
            }
            .distinctUntilChanged()
            .onEach { done ->
                if (done) {
                    WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> stop service")
                    productImagesServiceWrapper.stopService()
                    listOfImagesToUpload.clear()
                } else {
                    WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start service")
                    productImagesServiceWrapper.startService()
                }
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
        if (cancelledProducts.contains(work.productId)) return

        val job = appCoroutineScope.launch {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start work handling $work")

            try {
                when (work) {
                    is Work.FetchMedia -> fetchMedia(work)
                    is Work.UploadMedia -> uploadMedia(work)
                    is Work.UpdateProduct -> updateProduct(work)
                }
            } finally {
                currentWorkListCount.update { list -> list - work }
            }
        }

        // Save a reference to the job for cancelling it if needed
        currentJobs[work.productId] = currentJobs.getOrElse(work.productId) { emptyList() } + job

        job.invokeOnCompletion {
            // Remove the job from the list jobs
            currentJobs[work.productId] = currentJobs[work.productId]!! - job
        }
    }

    fun enqueueImagesUpload(productId: Long, uris: List<String>) {
        cancelledProducts.remove(productId)
        uris.forEach {
            queue.tryEmit(Work.FetchMedia(productId, it))
        }
    }

    fun addImagesToProduct(productId: Long, images: List<MediaModel>) {
        queue.tryEmit(Work.UpdateProduct(productId, images))
    }

    fun cancelUpload(productId: Long) {
        cancelledProducts.add(productId)
        currentJobs[productId]?.forEach {
            it.cancel()
        }
        listOfImagesToUpload.removeAll { it.first == productId }
    }

    private suspend fun fetchMedia(work: Work.FetchMedia) {
        emitEvent(
            Event.MediaUploadEvent.UploadStarted(
                productId = work.productId,
                localUri = work.localUri
            )
        )
        WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> fetch media ${work.localUri}")

        val fetchedMedia = mediaFilesRepository.fetchMedia(work.localUri)
        if (fetchedMedia == null) {
            WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> fetching media failed")

            emitEvent(
                Event.MediaUploadEvent.UploadFailed(
                    productId = work.productId,
                    localUri = work.localUri,
                    error = MediaFilesRepository.MediaUploadException(
                        media = MediaModel(),
                        errorMessage = resourceProvider.getString(R.string.product_image_service_error_media_null),
                        errorType = MediaStore.MediaErrorType.NULL_MEDIA_ARG
                    )
                )
            )
        } else {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> media fetched, enqueue upload")
            queue.emit(
                Work.UploadMedia(
                    productId = work.productId,
                    localUri = work.localUri,
                    fetchedMedia = fetchedMedia
                )
            )
            listOfImagesToUpload.add(Pair(work.productId, work.localUri))
        }
    }

    private suspend fun uploadMedia(work: Work.UploadMedia) {
        mutex.withLock {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start uploading media ${work.localUri}")

            val indexOfCurrentUpload = listOfImagesToUpload.indexOf(Pair(work.productId, work.localUri))
            notificationHandler.update(indexOfCurrentUpload + 1, listOfImagesToUpload.size)
            try {
                val uploadedMedia = mediaFilesRepository.uploadMedia(work.fetchedMedia)
                WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> upload succeeded for ${work.localUri}")
                emitEvent(
                    Event.MediaUploadEvent.UploadSucceeded(
                        productId = work.productId,
                        localUri = work.localUri,
                        media = uploadedMedia
                    )
                )
            } catch (e: MediaFilesRepository.MediaUploadException) {
                WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> upload failed for ${work.localUri}")
                emitEvent(
                    Event.MediaUploadEvent.UploadFailed(
                        productId = work.productId,
                        localUri = work.localUri,
                        error = e
                    )
                )
            }

            val hasMoreUploads = currentWorkListCount.value.any {
                it != work && it.productId == work.productId && (it is Work.UploadMedia || it is Work.FetchMedia)
            }
            if (!hasMoreUploads) {
                WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> all uploads for product ${work.productId} are done")
                emitEvent(Event.ProductUploadsCompleted(work.productId))
            }
        }
    }

    @Suppress("MagicNumber")
    private suspend fun updateProduct(work: Work.UpdateProduct) {
        suspend fun fetchProductWithRetries(productId: Long): Product? {
            var retries = 0
            while (retries < 3) {
                val product = productDetailRepository.fetchProduct(productId)
                if (product != null && productDetailRepository.lastFetchProductErrorType == null) {
                    return product
                }
                retries++
            }
            return null
        }

        suspend fun updateProductWithRetries(product: Product): Boolean {
            var retries = 0
            while (retries < 3) {
                val result = productDetailRepository.updateProduct(product)
                if (result) {
                    return true
                }
                retries++
            }
            return false
        }
        mutex.withLock {
            WooLog.d(T.MEDIA, "ProductImagesUploadWorker -> start updating product ${work.productId}")

            val images = work.addedImages.map { it.toAppModel() }

            notificationHandler.showUpdatingProductNotification(null)

            val product = fetchProductWithRetries(work.productId)
            if (product == null) {
                WooLog.w(T.MEDIA, "ProductImagesUploadWorker -> fetching product ${work.productId} failed")
                emitEvent(Event.ProductUpdateEvent.ProductUpdateFailed(work.productId, product))
            } else {
                notificationHandler.showUpdatingProductNotification(product)
                val result = updateProductWithRetries(product.copy(images = product.images + images))
                if (result) {
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
    }

    sealed class Work {
        abstract val productId: Long

        class FetchMedia(
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

        sealed class MediaUploadEvent : Event() {
            abstract val localUri: String

            data class UploadStarted(
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
                val product: Product? = null
            ) : ProductUpdateEvent()
        }
    }
}
