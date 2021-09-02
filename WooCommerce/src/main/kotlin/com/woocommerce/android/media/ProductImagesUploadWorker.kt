package com.woocommerce.android.media

import android.net.Uri
import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.media.MediaFileUploadHandler
import com.woocommerce.android.ui.products.ProductDetailRepository
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProductImagesUploadWorker @Inject constructor(
    private val mediaFilesRepository: MediaFilesRepository,
    private val productDetailRepository: ProductDetailRepository,
    private val resourceProvider: ResourceProvider,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope,
) {
    private val queue = MutableSharedFlow<Work>(extraBufferCapacity = Int.MAX_VALUE)
    private val currentWorkListCount = MutableStateFlow<List<Work>>(emptyList())

    private val _events = MutableSharedFlow<MediaFileUploadHandler.ProductImageUploadData>()
    val events = _events.asSharedFlow()

    private val cancelledProducts = mutableSetOf<Long>()
    private val currentJobs = mutableMapOf<Long, List<Job>>()

    private val mutex = Mutex()

    init {
        queue
            .onEach {
                currentWorkListCount.value += it
            }
            .onEach {
                handleWork(it)
            }
            .launchIn(appCoroutineScope)


        appCoroutineScope.launch {
            currentWorkListCount
                .map { it.isEmpty() }
                .distinctUntilChanged()
                .onEach { isEmpty ->
                    if (isEmpty) {
                        // Add a delay to ensure to avoid stopping the service if there is an event coming to the queue
                        delay(1000L)
                    }
                }
                .collectLatest { done ->
                    if (done) {
                        productImagesServiceWrapper.stopService()
                    } else {
                        productImagesServiceWrapper.startService()
                    }
                }
        }
    }

    private fun handleWork(work: Work) {
        if (cancelledProducts.contains(work.productId)) return

        val job = appCoroutineScope.launch {
            try {
                when (work) {
                    is Work.FetchMedia -> fetchMedia(work)
                    is Work.UploadMedia -> uploadMedia(work)
                    is Work.UpdateProduct -> updateProduct(work)
                }
            } catch (cancellationException: CancellationException) {
                // Continue
            }

            currentWorkListCount.value -= work
        }

        // Save a reference to the job for cancelling it if needed
        currentJobs[work.productId] = currentJobs.getOrElse(work.productId) { emptyList() } + job

        job.invokeOnCompletion {
            // Remove the job from the list jobs
            currentJobs[work.productId] = currentJobs[work.productId]!! - job
        }
    }

    private suspend fun fetchMedia(work: Work.FetchMedia) {
        _events.emit(
            MediaFileUploadHandler.ProductImageUploadData(
                remoteProductId = work.productId,
                localUri = work.localUri,
                uploadStatus = MediaFileUploadHandler.UploadStatus.InProgress
            )
        )
        val fetchedMedia = mediaFilesRepository.fetchMedia(work.localUri)
        if (fetchedMedia == null) {
            _events.emit(
                MediaFileUploadHandler.ProductImageUploadData(
                    remoteProductId = work.productId,
                    localUri = work.localUri,
                    uploadStatus = MediaFileUploadHandler.UploadStatus.Failed(
                        media = MediaModel(),
                        mediaErrorMessage = resourceProvider.getString(R.string.product_image_service_error_media_null),
                        mediaErrorType = MediaStore.MediaErrorType.NULL_MEDIA_ARG
                    )
                )
            )
        } else {
            queue.emit(
                Work.UploadMedia(
                    productId = work.productId,
                    localUri = work.localUri,
                    fetchedMedia = fetchedMedia
                )
            )
        }
    }

    private suspend fun uploadMedia(work: Work.UploadMedia) {
        mutex.withLock {
            try {
                val uploadedMedia = mediaFilesRepository.uploadMedia(work.fetchedMedia)
                _events.emit(
                    MediaFileUploadHandler.ProductImageUploadData(
                        remoteProductId = work.productId,
                        localUri = work.localUri,
                        uploadStatus = MediaFileUploadHandler.UploadStatus.UploadSuccess(uploadedMedia)
                    )
                )
            } catch (e: MediaFilesRepository.MediaUploadException) {
                _events.emit(
                    MediaFileUploadHandler.ProductImageUploadData(
                        remoteProductId = work.productId,
                        localUri = work.localUri,
                        uploadStatus = MediaFileUploadHandler.UploadStatus.Failed(
                            media = e.media,
                            mediaErrorMessage = e.errorMessage,
                            mediaErrorType = e.errorType
                        )
                    )
                )
            }
        }
    }

    fun enqueueImagesUpload(productId: Long, uris: List<Uri>) {
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
            val images = work.addedImages.map { it.toAppModel() }

            val product = fetchProductWithRetries(work.productId)
            if (product == null) {
                // TODO post failure notification notifHandler.postUpdateFailureNotification(productId, null)
            } else {
                val result = updateProductWithRetries(product.copy(images = product.images + images))
                if (result) {
                    // TODO post success notification notifHandler.postUpdateSuccessNotification(productId, product, images.size)
                } else {
                    // TODO post failure notification notifHandler.postUpdateFailureNotification(productId, product)
                }
            }
        }
    }

    sealed class Work {
        abstract val productId: Long

        class FetchMedia(
            override val productId: Long,
            val localUri: Uri
        ) : Work()

        data class UploadMedia(
            override val productId: Long,
            val localUri: Uri,
            val fetchedMedia: MediaModel,
        ) : Work()

        data class UpdateProduct(
            override val productId: Long,
            val addedImages: List<MediaModel>
        ) : Work()
    }
}
