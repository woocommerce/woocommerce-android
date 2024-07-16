package com.woocommerce.android.ui.products.ai.preview

import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.model.Image
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transform
import org.wordpress.android.fluxc.model.MediaModel
import javax.inject.Inject

class UploadImage @Inject constructor(
    private val mediaFilesRepository: MediaFilesRepository
) {
    suspend operator fun invoke(selectedImage: Image): Result<MediaModel> =
        when (selectedImage) {
            is Image.LocalImage -> mediaFilesRepository.uploadFile(selectedImage.uri)
                .transform {
                    when (it) {
                        is MediaFilesRepository.UploadResult.UploadSuccess -> emit(Result.success(it.media))
                        is MediaFilesRepository.UploadResult.UploadFailure -> throw it.error
                        else -> {
                            /* Do nothing */
                        }
                    }
                }
                .retry(1)
                .catch { emit(Result.failure(it)) }
                .first()

            is Image.WPMediaLibraryImage -> mediaFilesRepository.fetchWordPressMedia(selectedImage.content.id)
        }
}
