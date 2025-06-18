@file:Suppress("ImportOrdering")

package com.woocommerce.android.ui.woopos.common.composeui.component

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@HiltViewModel
class WooPosBarcodeProcessor @Inject constructor() : ViewModel() {
    private val scanner: BarcodeScanner by lazy {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    private val _barcodeResults = Channel<String>(capacity = 10)
    val barcodeResults: Flow<String> = _barcodeResults.receiveAsFlow()

    suspend fun processImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            try {
                val result = processImageInternal(image)
                result?.let { barcode ->
                    _barcodeResults.trySend(barcode)
                }
            } finally {
                imageProxy.close()
            }
        } else {
            imageProxy.close()
        }
    }

    private suspend fun processImageInternal(image: InputImage): String? = suspendCoroutine { cont ->
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                val barcode = barcodes.firstOrNull()?.rawValue
                cont.resume(barcode)
            }
            .addOnFailureListener {
                cont.resume(null)
            }
    }
}
