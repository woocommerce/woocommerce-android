package com.woocommerce.android.ui.products.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest.Builder
import coil.request.SuccessResult
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.korean.KoreanTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.woocommerce.android.util.WooLog
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import java.util.Locale
import javax.inject.Inject

class TextRecognitionEngine @Inject constructor(
    @ApplicationContext private val appContext: Context
) {
    private companion object {
        const val JAPANESE_LANGUAGE_CODE = "ja"
        const val CHINESE_LANGUAGE_CODE = "zh"
        const val KOREAN_LANGUAGE_CODE = "ko"
    }

    @Suppress("TooGenericExceptionCaught")
    suspend fun processImage(imageUrl: String): Result<List<String>> {
        return try {
            val bitmap = loadBitmap(imageUrl)
            val image = InputImage.fromBitmap(requireNotNull(bitmap), 0)
            val deviceLocale = Locale.getDefault().language
            val result = when (deviceLocale) {
                JAPANESE_LANGUAGE_CODE -> TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
                CHINESE_LANGUAGE_CODE -> TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
                KOREAN_LANGUAGE_CODE -> TextRecognition.getClient(KoreanTextRecognizerOptions.Builder().build())
                else -> TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            }.process(image).await()
            Result.success(result.textBlocks.map { it.text })
        } catch (e: Exception) {
            WooLog.d(WooLog.T.AI, "Failed to scan text from image: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun loadBitmap(imageUrl: String): Bitmap? {
        val loader = ImageLoader(appContext)
        val request = Builder(appContext)
            .data(imageUrl)
            .allowHardware(false) // Disable hardware bitmaps.
            .build()

        val result = (loader.execute(request) as SuccessResult).drawable
        return (result as BitmapDrawable).bitmap
    }
}
