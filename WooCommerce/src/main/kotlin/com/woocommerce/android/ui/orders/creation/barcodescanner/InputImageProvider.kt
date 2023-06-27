package com.woocommerce.android.ui.orders.creation.barcodescanner

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

@ExperimentalGetImage
class InputImageProvider @Inject constructor() {
    fun provideImage(imageProxy: ImageProxy): InputImage {
        return InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)
    }
}
