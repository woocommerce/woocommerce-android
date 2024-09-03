package com.woocommerce.android.ui.media

import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent.PRODUCT_IMAGE_UPLOAD_FAILED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.media.ProductImagesNotificationHandler
import com.woocommerce.android.media.ProductImagesUploadWorker
import com.woocommerce.android.media.ProductImagesUploadWorker.Event
import com.woocommerce.android.media.ProductImagesUploadWorker.Work
import com.woocommerce.android.media.ProductImagesUploadWorker.Work.UploadMedia
import com.woocommerce.android.ui.products.details.ProductDetailRepository
import com.woocommerce.android.ui.products.details.ProductDetailViewModel
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.update
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaFileUploadHandler @Inject constructor(
    private val notificationHandler: ProductImagesNotificationHandler,
    private val worker: ProductImagesUploadWorker,
    private val resourceProvider: ResourceProvider,
    private val productDetailRepository: ProductDetailRepository,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val uploadsStatus = MutableStateFlow(emptyList<ProductImageUploadData>())
    private val externalObservers = mutableListOf<Long>()

    init {
        worker.events
            .onEach { event ->
                WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> handling $event")
                when (event) {
                    is Event.MediaUploadEvent -> handleMediaUploadEvent(event)
                    is Event.ProductUploadsCompleted -> updateProductIfNeeded(event.productId)
                    is Event.ProductUpdateEvent -> handleProductUpdateEvent(event)
                    Event.ServiceStopped -> clearPendingUploads()
                }
            }
            .launchIn(appCoroutineScope)
    }

    private fun handleMediaUploadEvent(event: Event.MediaUploadEvent) {
        val statusList = uploadsStatus.value.toMutableList()
        val index = statusList.indexOfFirst {
            it.remoteProductId == event.productId && it.localUri == event.localUri
        }
        if (index == -1) {
            WooLog.w(WooLog.T.MEDIA, "MediaFileUploadHandler -> received event for unmatched media")
            return
        }

        val newStatus = event.toStatus()

        when (event) {
            is Event.MediaUploadEvent.FetchSucceeded -> {
                enqueueMediaUpload(event)
            }
            is Event.MediaUploadEvent.FetchFailed -> {
                statusList[index] = newStatus
                showUploadFailureNotifIfNoObserver(event.productId, statusList)
            }
            is Event.MediaUploadEvent.UploadSucceeded -> {
                if (externalObservers.contains(event.productId)) {
                    WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload successful, while handler is observed")
                    statusList.removeAt(index)
                } else {
                    WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload successful with no observers")
                    statusList[index] = newStatus
                }
            }
            is Event.MediaUploadEvent.UploadFailed -> {
                WooLog.e(WooLog.T.MEDIA, "MediaFileUploadHandler -> Upload failed", event.error)
                statusList[index] = newStatus
                AnalyticsTracker.track(
                    PRODUCT_IMAGE_UPLOAD_FAILED,
                    mapOf(
                        AnalyticsTracker.KEY_ERROR_CONTEXT to this::class.java.simpleName,
                        AnalyticsTracker.KEY_ERROR_TYPE to event.error.errorType.toString(),
                        AnalyticsTracker.KEY_ERROR_DESC to event.error.message
                    )
                )
                showUploadFailureNotifIfNoObserver(event.productId, statusList)
            }
        }
        uploadsStatus.value = statusList
    }

    private fun enqueueMediaUpload(event: Event.MediaUploadEvent.FetchSucceeded) {
        worker.enqueueWork(
            UploadMedia(
                productId = event.productId,
                localUri = event.localUri,
                fetchedMedia = event.fetchedMedia
            )
        )
    }

    private fun handleProductUpdateEvent(event: Event.ProductUpdateEvent) {
        when (event) {
            is Event.ProductUpdateEvent.ProductUpdateFailed ->
                notificationHandler.postUpdateFailureNotification(event.productId, event.product)
            is Event.ProductUpdateEvent.ProductUpdateSucceeded ->
                notificationHandler.postUpdateSuccessNotification(event.productId, event.product, event.imagesCount)
        }
    }

    private fun updateProductIfNeeded(productId: Long) {
        WooLog.d(
            tag = WooLog.T.MEDIA,
            message = "MediaFileUploadHandler -> uploads finished for product $productId, check if we need to update it"
        )
        uploadsStatus.value.filter { it.remoteProductId == productId && it.uploadStatus !is UploadStatus.Failed }
            .takeIf { images -> images.none { it.uploadStatus == UploadStatus.InProgress } && images.isNotEmpty() }
            ?.let { productImages ->
                val uploadedImages = productImages.map { (it.uploadStatus as UploadStatus.UploadSuccess).media }

                WooLog.d(
                    WooLog.T.MEDIA,
                    "MediaFileUploadHandler -> add ${uploadedImages.size} images to the product"
                )
                worker.enqueueWork(Work.UpdateProduct(productId, uploadedImages))

                uploadsStatus.update { list -> list - productImages }
            }
    }

    private fun showUploadFailureNotifIfNoObserver(productId: Long, state: List<ProductImageUploadData>) {
        if (!externalObservers.contains(productId)) {
            WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> post upload failure notification")
            val errors = state.filter { it.remoteProductId == productId && it.uploadStatus is UploadStatus.Failed }
            notificationHandler.postUploadFailureNotification(productDetailRepository.getProductFromLocalCache(productId), errors)
        }
    }

    private fun clearPendingUploads() {
        uploadsStatus.update { list ->
            list.filterNot {
                it.uploadStatus is UploadStatus.InProgress || it.uploadStatus is UploadStatus.UploadSuccess
            }
        }
    }

    fun enqueueUpload(remoteProductId: Long, uris: List<String>) {
        uploadsStatus.update { list ->
            list + uris.map {
                ProductImageUploadData(
                    remoteProductId = remoteProductId,
                    localUri = it,
                    uploadStatus = UploadStatus.InProgress
                )
            }
        }
        uris.forEach {
            worker.enqueueWork(Work.FetchMedia(remoteProductId, it))
        }
    }

    fun cancelUpload(remoteProductId: Long) {
        uploadsStatus.update { list -> list.filterNot { it.remoteProductId == remoteProductId } }

        worker.cancelUpload(remoteProductId)
    }

    fun clearImageErrors(remoteProductId: Long) {
        uploadsStatus.update { list ->
            list.filterNot {
                it.remoteProductId == remoteProductId && it.uploadStatus is UploadStatus.Failed
            }
        }
        notificationHandler.removeUploadFailureNotification(remoteProductId)
    }

    fun observeCurrentUploadErrors(remoteProductId: Long): Flow<List<ProductImageUploadData>> =
        uploadsStatus.map { list ->
            list.filter { it.remoteProductId == remoteProductId && it.uploadStatus is UploadStatus.Failed }
        }

    fun observeCurrentUploads(remoteProductId: Long): Flow<List<String>> {
        return uploadsStatus
            .map { list ->
                list.filter { it.remoteProductId == remoteProductId && it.uploadStatus == UploadStatus.InProgress }
                    .map { it.localUri }
            }
    }

    fun observeSuccessfulUploads(remoteProductId: Long): Flow<MediaModel> {
        return worker.events
            .onSubscription { externalObservers.add(remoteProductId) }
            .onCompletion { externalObservers.remove(remoteProductId) }
            .filterIsInstance<Event.MediaUploadEvent.UploadSucceeded>()
            .filter { it.productId == remoteProductId }
            .map { it.media }
            .onStart {
                // Start with the pending succeeded uploads, the observer will be able to handle them
                val pendingSuccessUploads = uploadsStatus.value.filter {
                    it.remoteProductId == remoteProductId && it.uploadStatus is UploadStatus.UploadSuccess
                }
                if (pendingSuccessUploads.isNotEmpty()) {
                    uploadsStatus.update { list -> list - pendingSuccessUploads }
                    pendingSuccessUploads.forEach {
                        emit((it.uploadStatus as UploadStatus.UploadSuccess).media)
                    }
                }
            }
    }

    fun observeProductImageChanges(): Flow<Long> {
        return worker.events
            .filterIsInstance<Event.ProductUploadsCompleted>()
            .map { it.productId }
    }

    fun assignUploadsToCreatedProduct(productId: Long) {
        WooLog.d(WooLog.T.MEDIA, "MediaFileUploadHandler -> assign uploads to the created product $productId")
        // Update id for past successful uploads
        uploadsStatus.update { list ->
            list.map {
                if (it.remoteProductId == ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID &&
                    it.uploadStatus is UploadStatus.UploadSuccess
                ) {
                    it.copy(remoteProductId = productId)
                } else {
                    it
                }
            }
        }
        // Cancel and reschedule ongoing uploads
        val ongoingUploads = uploadsStatus.value.filter {
            it.remoteProductId == ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID && it.uploadStatus ==
                UploadStatus.InProgress
        }
        if (ongoingUploads.isNotEmpty()) {
            cancelUpload(ProductDetailViewModel.DEFAULT_ADD_NEW_PRODUCT_ID)
            enqueueUpload(productId, ongoingUploads.map { it.localUri })
        }
    }

    private fun Event.MediaUploadEvent.toStatus(): ProductImageUploadData {
        val uploadStatus = when (this) {
            is Event.MediaUploadEvent.FetchFailed -> UploadStatus.Failed(
                mediaErrorMessage = resourceProvider.getString(R.string.product_image_service_error_media_null),
                mediaErrorType = MediaStore.MediaErrorType.NULL_MEDIA_ARG
            )
            is Event.MediaUploadEvent.FetchSucceeded -> UploadStatus.InProgress
            is Event.MediaUploadEvent.UploadFailed -> UploadStatus.Failed(
                media = error.media,
                mediaErrorMessage = error.errorMessage,
                mediaErrorType = error.errorType
            )
            is Event.MediaUploadEvent.UploadSucceeded -> UploadStatus.UploadSuccess(media = media)
        }
        return ProductImageUploadData(
            remoteProductId = productId,
            localUri = localUri,
            uploadStatus = uploadStatus
        )
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
            val media: MediaModel? = null,
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
