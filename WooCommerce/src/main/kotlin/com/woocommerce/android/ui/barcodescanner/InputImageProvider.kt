package com.woocommerce.android.ui.barcodescanner

import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject


interface InputImageProvider {
    fun provideImage(imageProxy: ImageProxy): InputImage
}
class BitmapImageProvider @Inject constructor() : InputImageProvider {
    override fun provideImage(imageProxy: ImageProxy): InputImage {
        return InputImage.fromBitmap(imageProxy.toBitmap(), imageProxy.imageInfo.rotationDegrees)
    }
}
