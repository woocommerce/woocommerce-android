package com.woocommerce.android.ui.products.images.ai

import android.content.Context
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class CreateInputImageFromUrl @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(imageUrl: String): Result<InputImage> {
        return try {
            val imageLoader = ImageLoader(context)
            val imageRequest = ImageRequest.Builder(context)
                .data(imageUrl)
                .build()

            val result = imageLoader.execute(imageRequest)
            when (result) {
                is SuccessResult -> {
                    val bitmap = result.drawable.toBitmap()
                    val inputImage = InputImage.fromBitmap(bitmap, 0)
                    Result.success(inputImage)
                }
                else -> Result.failure(Exception("Image loading failed"))
            }
        } catch (e: IllegalArgumentException) {
            Result.failure(e)
        } catch (e: OutOfMemoryError) {
            Result.failure(Exception("Out of memory loading image", e))
        }
    }
}
