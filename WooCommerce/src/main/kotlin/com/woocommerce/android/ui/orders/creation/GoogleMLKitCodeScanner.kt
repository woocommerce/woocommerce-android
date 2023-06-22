package com.woocommerce.android.ui.orders.creation

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject

@ExperimentalGetImage
class GoogleMLKitCodeScanner @Inject constructor(
    private val barcodeScanner: BarcodeScanner,
    private val imageProxy: ImageProxy,
    private val errorMapper: GoogleCodeScannerErrorMapper,
    private val barcodeFormatMapper: GoogleBarcodeFormatMapper,
) : CodeScanner {
    override fun startScan(): Flow<CodeScannerStatus> {
        return callbackFlow {
            val inputImage = InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
            barcodeScanner.process(inputImage)
                .addOnSuccessListener { barcodeList ->
                    handleScanSuccess(barcodeList.firstOrNull())
                    this@callbackFlow.close()
                }
                .addOnFailureListener { exception ->
                    this@callbackFlow.trySend(
                        CodeScannerStatus.Failure(
                            error = exception.message,
                            type = errorMapper.mapGoogleMLKitScanningErrors(exception)
                        )
                    )
                    this@callbackFlow.close()
                }
                .addOnCompleteListener {
                    // When the image is from CameraX analysis use case, must call image.close() on received
                    // images when finished using them. Otherwise, new images may not be received or the camera
                    // may stall.
                    imageProxy.close()
                }
            awaitClose()
        }
    }

    private fun ProducerScope<CodeScannerStatus>.handleScanSuccess(code: Barcode?) {
        code?.rawValue?.let {
            trySend(
                CodeScannerStatus.Success(
                    it,
                    barcodeFormatMapper.mapBarcodeFormat(code.format)
                )
            )
        } ?: run {
            trySend(
                CodeScannerStatus.Failure(
                    error = "Failed to find a valid raw value!",
                    type = CodeScanningErrorType.Other(Throwable("Empty raw value"))
                )
            )
        }
    }
}
