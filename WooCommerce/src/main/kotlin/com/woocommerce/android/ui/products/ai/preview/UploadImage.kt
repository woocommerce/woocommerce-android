package com.woocommerce.android.ui.products.ai.preview

import com.woocommerce.android.media.MediaFilesRepository
import com.woocommerce.android.model.Image
import com.woocommerce.android.model.Product
import com.woocommerce.android.model.toAppModel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.retry
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

class UploadImage @Inject constructor(
    private val mediaFilesRepository: MediaFilesRepository
) {
    suspend operator fun invoke(selectedImage: Image): Result<Product.Image> =
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
                .map { it.toAppModel() }

            is Image.WPMediaLibraryImage -> Result.success(selectedImage.content)
        }
}
