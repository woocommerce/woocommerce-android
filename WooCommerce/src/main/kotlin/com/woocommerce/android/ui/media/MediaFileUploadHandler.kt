package com.woocommerce.android.ui.media

import android.net.Uri
import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.di.AppCoroutineScope
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploadFailed
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.model.toAppModel
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.Failed
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.InProgress
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.UploadSuccess
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.parcelize.Parcelize
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaFileUploadHandler @Inject constructor(
    private val resourceProvider: ResourceProvider,
    private val productImagesServiceWrapper: ProductImagesServiceWrapper,
    @AppCoroutineScope private val appCoroutineScope: CoroutineScope
) {
    private val uploadsStatus = MutableStateFlow(emptyList<ProductImageUploadData>())

    private val events = MutableSharedFlow<ProductImageUploadData>(extraBufferCapacity = Int.MAX_VALUE)

    private val externalObservers = mutableSetOf<Long>()

    init {
        EventBus.getDefault().register(this)
        events
            .onEach { event ->
                val statusList = uploadsStatus.value.toMutableList()
                val index = statusList.indexOfFirst {
                    it.remoteProductId == event.remoteProductId && it.localUri == event.localUri
                }
                if (index == -1) return@onEach

                if (event.uploadStatus is UploadSuccess && externalObservers.contains(event.remoteProductId)) {
                    statusList.removeAt(index)
                } else {
                    statusList[index] = event
                }
                uploadsStatus.value = statusList

                if (event.uploadStatus is Failed && !externalObservers.contains(event.remoteProductId)) {
                    uploadFailureNotification(event.remoteProductId, statusList)
                }
                updateProductIfNeeded(event.remoteProductId, statusList)
            }
            .launchIn(appCoroutineScope)
    }

    private fun updateProductIfNeeded(productId: Long, state: List<ProductImageUploadData>) {
        val productImages = state.filter { it.remoteProductId == productId && it.uploadStatus !is Failed }
        if (productImages.none { it.uploadStatus == InProgress }) {
            val uploadedImages = productImages.filter { it.uploadStatus is UploadSuccess }
                .map { (it.uploadStatus as UploadSuccess).media.toAppModel() }

            if (uploadedImages.isNotEmpty()) {
                productImagesServiceWrapper.addImagesToProduct(productId, uploadedImages)
            }

            uploadsStatus.value -= productImages
        }
    }

    private fun uploadFailureNotification(productId: Long, state: List<ProductImageUploadData>) {
        val errorsCount = state.filter { it.remoteProductId == productId && it.uploadStatus is Failed }.size
        productImagesServiceWrapper.showUploadFailureNotification(productId, errorsCount)
    }

    fun enqueueUpload(remoteProductId: Long, uris: List<Uri>) {
        val newUploads = uris.map {
            ProductImageUploadData(
                remoteProductId = remoteProductId,
                localUri = it,
                uploadStatus = InProgress
            )
        }
        uploadsStatus.value += newUploads
        productImagesServiceWrapper.uploadProductMedia(remoteProductId, ArrayList(uris))
    }

    fun cancelUpload(remoteProductId: Long) {
        uploadsStatus.value = uploadsStatus.value.filterNot { it.remoteProductId == remoteProductId }

        ProductImagesService.cancel(remoteProductId)
    }

    fun clearImageErrors(remoteProductId: Long) {
        uploadsStatus.value = uploadsStatus.value.filterNot {
            it.remoteProductId == remoteProductId && it.uploadStatus is Failed
        }
        productImagesServiceWrapper.removeUploadFailureNotification(remoteProductId)
    }

    fun observeCurrentUploadErrors(remoteProductId: Long): Flow<List<ProductImageUploadData>> =
        uploadsStatus.map { list ->
            list.filter { it.remoteProductId == remoteProductId && it.uploadStatus is Failed }
        }.filter { it.isNotEmpty() }

    fun observeCurrentUploads(remoteProductId: Long): Flow<List<Uri>> {
        return uploadsStatus
            .map { list ->
                list.filter { it.remoteProductId == remoteProductId && it.uploadStatus == InProgress }
                    .map { it.localUri }
            }
    }

    fun observeSuccessfulUploads(remoteProductId: Long): Flow<MediaModel> {
        return events
            .onSubscription { externalObservers.add(remoteProductId) }
            .onCompletion { externalObservers.remove(remoteProductId) }
            .filter { it.remoteProductId == remoteProductId && it.uploadStatus is UploadSuccess }
            .map { (it.uploadStatus as UploadSuccess).media }
    }

    /**
     * A single product image has finished uploading
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploaded) {
        val productId = event.media.postId
        events.tryEmit(
            ProductImageUploadData(
                remoteProductId = productId,
                localUri = event.localUri,
                uploadStatus = UploadStatus.UploadSuccess(media = event.media)
            )
        )
    }

    /**
     * image upload failed
     */
    @Suppress("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onEventMainThread(event: OnProductImageUploadFailed) {
        val productId = event.media.postId
        val errorMessage = event.error.message
            ?: event.error.logMessage
            ?: resourceProvider.getString(R.string.product_image_service_error_uploading)

        events.tryEmit(
            ProductImageUploadData(
                remoteProductId = productId,
                localUri = event.localUri,
                uploadStatus = Failed(event.media, event.error.type, errorMessage)
            )
        )
    }

    /***
     * Identifies both an event and status.
     * Holds a reference to the productId and localUri to keep track of each upload
     */
    @Parcelize
    data class ProductImageUploadData(
        val remoteProductId: Long,
        val localUri: Uri,
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
