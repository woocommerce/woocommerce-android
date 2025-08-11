package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import javax.inject.Inject

/**
 * Use case that orchestrates the complete background removal process:
 * 1. Load image from URL and create InputImage
 * 2. Remove background using ML Kit
 * 3. Return the processed bitmap
 */
class PerformImageBackgroundRemoval @Inject constructor(
    private val createInputImageFromUrl: CreateInputImageFromUrl,
    private val removeBackground: RemoveBackground
) {
    suspend operator fun invoke(imageUrl: String): Result<Bitmap> {
        return createInputImageFromUrl(imageUrl).fold(
            onSuccess = { inputImage ->
                removeBackground(inputImage)
            },
            onFailure = { error ->
                Result.failure(error)
            }
        )
    }
}
