package com.woocommerce.android.ui.orders.creation

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.woocommerce.android.ui.barcodescanner.MediaImageProvider
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class GoogleMLKitCodeScanner @Inject constructor(
    private val barcodeScanner: BarcodeScanner,
    private val errorMapper: GoogleCodeScannerErrorMapper,
    private val barcodeFormatMapper: GoogleBarcodeFormatMapper,
    private val inputImageProvider: MediaImageProvider,
) : CodeScanner {
    override suspend fun recogniseCode(imageProxy: ImageProxy): CodeScannerStatus = suspendCoroutine { cont ->
        @androidx.camera.core.ExperimentalGetImage
        val image = inputImageProvider.provideImage(imageProxy)

        val barcodeTask = barcodeScanner.process(image)

        barcodeTask.addOnCompleteListener {
            // We must call image.close() on received images when finished using them.
            // Otherwise, new images may not be received or the camera may stall.
            imageProxy.close()
        }
        barcodeTask.addOnSuccessListener { barcodeList ->
            cont.resume(
                if (!barcodeList.isNullOrEmpty()) {
                    handleScanSuccess(barcodeList.firstOrNull())
                } else {
                    CodeScannerStatus.NotFound
                }
            )
        }
        barcodeTask.addOnFailureListener { exception ->
            cont.resume(
                CodeScannerStatus.Failure(
                    error = exception.message,
                    type = errorMapper.mapGoogleMLKitScanningErrors(exception)
                )
            )
        }
    }

    private fun handleScanSuccess(code: Barcode?) =
        code?.rawValue?.let {
            CodeScannerStatus.Success(
                it,
                barcodeFormatMapper.mapBarcodeFormat(code.format)
            )
        } ?: run {
            CodeScannerStatus.Failure(
                error = "Failed to find a valid raw value!",
                type = CodeScanningErrorType.Other(Throwable("Empty raw value"))
            )
        }
}
