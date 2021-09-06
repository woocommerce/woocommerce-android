package com.woocommerce.android.ui.media

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.extensions.update
import com.woocommerce.android.media.ProductImagesNotificationHandler
import com.woocommerce.android.media.ProductImagesUploadWorker
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.media.ProductImagesUploadWorker.Work.FetchMedia
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.*
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
@Suppress("TooManyFunctions")
@ExperimentalCoroutinesApi
class MediaFileUploadHandler @Inject constructor(
    private val notificationHandler: ProductImagesNotificationHandler,
    private val worker: ProductImagesUploadWorker,
    private val resourceProvider: ResourceProvider,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val uploadsStatus = MutableStateFlow(emptyList<ProductImageUploadData>())
    private val externalObservers = mutableListOf<Long>()

    init {
        worker.events
            .onEach { event ->
                WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> handling $event")
                when (event) {
                    is ProductImagesUploadWorker.Event.MediaUploadEvent -> handleMediaUploadEvent(event)
                    is ProductImagesUploadWorker.Event.ProductUploadsCompleted -> updateProductIfNeeded(event.productId)
                    is ProductImagesUploadWorker.Event.ProductUpdateEvent -> handleProductUpdateEvent(event)
                }
            }
            .launchIn(appCoroutineScope)
    }

    private fun handleMediaUploadEvent(event: ProductImagesUploadWorker.Event.MediaUploadEvent) {
        val statusList = uploadsStatus.value.toMutableList()
        val index = statusList.indexOfFirst {
            it.remoteProductId == event.productId && it.localUri == event.localUri
        }
        if (index == -1) {
            WooLog.w(WooLog.T.MEDIA, "MediaFileUploadHandler -> received event for unmatched media")
        }

        when (event) {
            is ProductImagesUploadWorker.Event.MediaUploadEvent.FetchSucceeded -> {
                worker.enqueueWork(
                    Work.UploadMedia(
                        productId = event.productId,
                        localUri = event.localUri,
                        fetchedMedia = event.fetchedMedia
                    )
                )
            }
            is ProductImagesUploadWorker.Event.MediaUploadEvent.FetchFailed -> {
                statusList[index] = ProductImageUploadData(
                    remoteProductId = event.productId,
                    localUri = event.localUri,
                    uploadStatus = Failed(
                        media = MediaModel(),
                        mediaErrorMessage = resourceProvider.getString(R.string.product_image_service_error_media_null),
                        mediaErrorType = MediaStore.MediaErrorType.NULL_MEDIA_ARG
                    )
                )
            }
            is ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded -> {
                if (externalObservers.contains(event.productId)) {
                    WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload successful, while handler is observed")
                    statusList.removeAt(index)
                } else {
                    WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload successful with no observers")
                    statusList[index] = ProductImageUploadData(
                        remoteProductId = event.productId,
                        localUri = event.localUri,
                        uploadStatus = UploadSuccess(media = event.media)
                    )
                }
            }
            is ProductImagesUploadWorker.Event.MediaUploadEvent.UploadFailed -> {
                WooLog.e(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload failed", event.error)
                statusList[index] = ProductImageUploadData(
                    remoteProductId = event.productId,
                    localUri = event.localUri,
                    uploadStatus = Failed(
                        media = event.error.media,
                        mediaErrorMessage = event.error.errorMessage,
                        mediaErrorType = event.error.errorType
                    )
                )
                if (!externalObservers.contains(event.productId)) {
                    WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> post upload failure notification")
                    showUploadFailureNotification(event.productId, statusList)
                }
            }
        }
        uploadsStatus.value = statusList
    }

    private fun handleProductUpdateEvent(event: ProductImagesUploadWorker.Event.ProductUpdateEvent) {
        when (event) {
            is ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateFailed ->
                notificationHandler.postUpdateFailureNotification(event.productId, event.product)
            is ProductImagesUploadWorker.Event.ProductUpdateEvent.ProductUpdateSucceeded ->
                notificationHandler.postUpdateSuccessNotification(event.productId, event.product, event.imagesCount)
        }
    }

    private fun updateProductIfNeeded(productId: Long) {
        WooLog.d(
            tag = WooLog.T.MEDIA,
            message = "MediaFileUploadHandler -> uploads finished for product $productId, check if we need to update it"
        )
        val state = uploadsStatus.value
        val productImages = state.filter { it.remoteProductId == productId && it.uploadStatus !is Failed }
        if (productImages.none { it.uploadStatus == InProgress }) {
            val uploadedImages = productImages.filter { it.uploadStatus is UploadSuccess }
                .map { (it.uploadStatus as UploadSuccess).media }

            if (uploadedImages.isNotEmpty()) {
                WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> add ${uploadedImages.size} images to the product")
                worker.enqueueWork(Work.UpdateProduct(productId, uploadedImages))
            }

            uploadsStatus.update { list -> list - productImages }
        }
    }

    private fun showUploadFailureNotification(productId: Long, state: List<ProductImageUploadData>) {
        val errorsCount = state.filter { it.remoteProductId == productId && it.uploadStatus is Failed }.size
        notificationHandler.postUploadFailureNotification(productId, errorsCount)
    }

    fun enqueueUpload(remoteProductId: Long, uris: List<String>) {
        uploadsStatus.update { list ->
            list + uris.map {
                ProductImageUploadData(
                    remoteProductId = remoteProductId,
                    localUri = it,
                    uploadStatus = InProgress
                )
            }
        }
        uris.forEach {
            worker.enqueueWork(FetchMedia(remoteProductId, it))
        }
    }

    fun cancelUpload(remoteProductId: Long) {
        uploadsStatus.update { list -> list.filterNot { it.remoteProductId == remoteProductId } }

        worker.cancelUpload(remoteProductId)
    }

    fun clearImageErrors(remoteProductId: Long) {
        uploadsStatus.update { list ->
            list.filterNot {
                it.remoteProductId == remoteProductId && it.uploadStatus is Failed
            }
        }
        notificationHandler.removeUploadFailureNotification(remoteProductId)
    }

    fun observeCurrentUploadErrors(remoteProductId: Long): Flow<List<ProductImageUploadData>> =
        uploadsStatus.map { list ->
            list.filter { it.remoteProductId == remoteProductId && it.uploadStatus is Failed }
        }.filter { it.isNotEmpty() }

    fun observeCurrentUploads(remoteProductId: Long): Flow<List<String>> {
        return uploadsStatus
            .map { list ->
                list.filter { it.remoteProductId == remoteProductId && it.uploadStatus == InProgress }
                    .map { it.localUri }
            }
    }

    fun observeSuccessfulUploads(remoteProductId: Long): Flow<MediaModel> {
        return worker.events
            .onSubscription { externalObservers.add(remoteProductId) }
            .onCompletion { externalObservers.remove(remoteProductId) }
            .filterIsInstance<ProductImagesUploadWorker.Event.MediaUploadEvent.UploadSucceeded>()
            .filter { it.productId == remoteProductId }
            .map { it.media }
            .onStart {
                // Start with the pending succeeded uploads, the observer will be able to handle them
                val pendingSuccessUploads = uploadsStatus.value.filter {
                    it.remoteProductId == remoteProductId && it.uploadStatus is UploadSuccess
                }
                if (pendingSuccessUploads.isNotEmpty()) {
                    uploadsStatus.update { list -> list - pendingSuccessUploads }
                    pendingSuccessUploads.forEach {
                        emit((it.uploadStatus as UploadSuccess).media)
                    }
                }
            }
    }

    fun observeProductImageChanges(): Flow<Long> {
        return worker.events
            .filterIsInstance<ProductImagesUploadWorker.Event.ProductUploadsCompleted>()
            .map { it.productId }
    }

    /***
     * Identifies both an event and status.
     * Holds a reference to the productId and localUri to keep track of each upload
     */
    @Parcelize
    data class ProductImageUploadData(
        val remoteProductId: Long,
        val localUri: String,
        val uploadStatus: UploadStatus
    ) : Parcelable

    sealed class UploadStatus : Parcelable {
        @Parcelize
        object InProgress : UploadStatus()

        @Parcelize
        data class Failed(
            val media: MediaModel,
            val mediaErrorType: MediaStore.MediaErrorType,
            val mediaErrorMessage: String
        ) : UploadStatus()

        @Parcelize
        data class UploadSuccess(val media: MediaModel) : UploadStatus()
    }
}

fun ResourceProvider.getMediaUploadErrorMessage(errorsCount: Int): String {
    return StringUtils.getQuantityString(
        resourceProvider = this,
        quantity = errorsCount,
        default = R.string.product_image_service_error_uploading_multiple,
        one = R.string.product_image_service_error_uploading_single,
        zero = R.string.product_image_service_error_uploading
    )
}
