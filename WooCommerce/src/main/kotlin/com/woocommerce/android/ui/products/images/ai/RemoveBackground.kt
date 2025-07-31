package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import android.graphics.Color
import androidx.core.graphics.createBitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import com.woocommerce.android.util.CoroutineDispatchers
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class RemoveBackground @Inject constructor(
    private val dispatchers: CoroutineDispatchers
) {
    suspend operator fun invoke(inputImage: InputImage): Result<Bitmap> =
        withContext(dispatchers.io) {
            suspendCancellableCoroutine { continuation ->
                val options = SubjectSegmenterOptions.Builder()
                    .enableForegroundBitmap()
                    .enableForegroundConfidenceMask()
                    .build()

                val segmenter = SubjectSegmentation.getClient(options)

                segmenter.process(inputImage)
                    .addOnSuccessListener { result ->
                        val foregroundBitmap = result.foregroundBitmap
                        if (foregroundBitmap != null) {
                            val processedBitmap = replaceBackgroundWithWhite(foregroundBitmap)
                            continuation.resume(Result.success(processedBitmap))
                        } else {
                            continuation.resume(Result.failure(Exception("No subject detected in image")))
                        }
                    }
                    .addOnFailureListener { exception ->
                        continuation.resume(Result.failure(exception))
                    }
                    .addOnCanceledListener {
                        continuation.resume(Result.failure(Exception("Background removal was cancelled")))
                    }
            }
        }

    private fun replaceBackgroundWithWhite(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)

        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        for (i in pixels.indices) {
            val alpha = Color.alpha(pixels[i])
            if (alpha == 0) {
                pixels[i] = Color.WHITE
            }
        }

        val resultBitmap = createBitmap(width, height)
        resultBitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return resultBitmap
    }
}
