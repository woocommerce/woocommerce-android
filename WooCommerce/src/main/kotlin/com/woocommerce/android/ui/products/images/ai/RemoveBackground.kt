package com.woocommerce.android.ui.products.images.ai

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.coroutines.resume

class RemoveBackground @Inject constructor() {
    suspend operator fun invoke(inputImage: InputImage): Result<Bitmap> = withContext(Dispatchers.Default) {
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
                        continuation.resume(Result.success(foregroundBitmap))
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
}
