package com.woocommerce.android.ui.media

import android.net.Uri
import android.os.Parcelable
import com.woocommerce.android.R
import com.woocommerce.android.media.ProductImagesService
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploadFailed
import com.woocommerce.android.media.ProductImagesService.Companion.OnProductImageUploaded
import com.woocommerce.android.media.ProductImagesServiceWrapper
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.Failed
import com.woocommerce.android.ui.media.MediaFileUploadHandler.UploadStatus.InProgress
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.coroutines.GlobalScope
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
    private val productImagesServiceWrapper: ProductImagesServiceWrapper
) {
    private val uploadsStatus = MutableStateFlow(emptyList<ProductImageUploadData>())

    private val events = MutableSharedFlow<ProductImageUploadData>(extraBufferCapacity = Int.MAX_VALUE)

    init {
        EventBus.getDefault().register(this)
        events
            .onEach { event ->
                val statusList = uploadsStatus.value.toMutableList()
                val index = statusList.indexOfFirst {
                    it.remoteProductId == event.remoteProductId && it.localUri == event.localUri
                }
                if (index == -1) return@onEach
                statusList[index] = event
                uploadsStatus.value = statusList
            }
            .launchIn(GlobalScope)
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

        // TODO update the service to cancel upload per product
        ProductImagesService.cancel()
    }

    fun clearImageErrors(remoteProductId: Long) {
        uploadsStatus.value = uploadsStatus.value.filterNot {
            it.remoteProductId == remoteProductId && it.uploadStatus is Failed
        }
    }

    fun observeCurrentUploadErrors(remoteProductId: Long): Flow<List<ProductImageUploadData>> =
        uploadsStatus.map { list ->
            list.filter { it.remoteProductId == remoteProductId && it.uploadStatus is Failed }
        }

    fun observeCurrentUploads(remoteProductId: Long): Flow<List<Uri>> {
        return uploadsStatus
            .map { list ->
                list.filter { it.remoteProductId == remoteProductId && it.uploadStatus == InProgress }
                    .map { it.localUri }
            }
    }

    fun observeUploadEvents(remoteProductId: Long): Flow<ProductImageUploadData> {
        return events.filter { it.remoteProductId == remoteProductId }
    }

    fun getMediaUploadErrorMessage(remoteProductId: Long): String {
        val errorsCount = uploadsStatus.value
            .filter { it.remoteProductId == remoteProductId && it.uploadStatus is Failed }
            .size

        return StringUtils.getQuantityString(
            resourceProvider = resourceProvider,
            quantity = errorsCount,
            default = R.string.product_image_service_error_uploading_multiple,
            one = R.string.product_image_service_error_uploading_single,
            zero = R.string.product_image_service_error_uploading
        )
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
        events.tryEmit(
            ProductImageUploadData(
                remoteProductId = productId,
                localUri = event.localUri,
                uploadStatus = Failed(event.media, event.error.type, event.error.message)
            )
        )
    }

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
