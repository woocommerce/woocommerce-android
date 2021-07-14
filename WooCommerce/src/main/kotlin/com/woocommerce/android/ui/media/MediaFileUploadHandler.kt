package com.woocommerce.android.ui.media

import android.os.Parcelable
import androidx.collection.LongSparseArray
import com.woocommerce.android.R
import com.woocommerce.android.util.StringUtils
import com.woocommerce.android.viewmodel.ResourceProvider
import kotlinx.parcelize.Parcelize
import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.store.MediaStore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaFileUploadHandler @Inject constructor(
    private val resourceProvider: ResourceProvider
) {
    // array of ID / images that have failed to upload for that product
    private val currentUploadErrors = LongSparseArray<List<ProductImageUploadUiModel>>()

    fun getMediaUploadErrorCount(remoteProductId: Long) = currentUploadErrors.get(remoteProductId)?.size ?: 0

    fun getMediaUploadErrors(remoteProductId: Long) = currentUploadErrors.get(remoteProductId)

    fun handleMediaUploadFailure(
        mediaModel: MediaModel,
        mediaUploadError: MediaStore.MediaError
    ) {
        val newErrors = currentUploadErrors.get(mediaModel.postId, mutableListOf()) +
            ProductImageUploadUiModel(mediaModel, mediaUploadError.type, mediaUploadError.message)
        currentUploadErrors.put(mediaModel.postId, newErrors)
    }

    fun getMediaUploadErrorMessage(remoteProductId: Long): String {
        return StringUtils.getQuantityString(
            resourceProvider = resourceProvider,
            quantity = getMediaUploadErrorCount(remoteProductId),
            default = R.string.product_image_service_error_uploading_multiple,
            one = R.string.product_image_service_error_uploading_single,
            zero = R.string.product_image_service_error_uploading
        )
    }

    @Parcelize
    data class ProductImageUploadUiModel(
        val media: MediaModel,
        val mediaErrorType: MediaStore.MediaErrorType,
        val mediaErrorMessage: String
    ) : Parcelable
}
