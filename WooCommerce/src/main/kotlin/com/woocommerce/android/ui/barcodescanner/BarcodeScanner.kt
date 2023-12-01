package com.woocommerce.android.ui.barcodescanner

import android.content.res.Configuration
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import androidx.camera.core.Preview as CameraPreview

@Composable
fun BarcodeScanner(
    onNewFrame: (ImageProxy) -> Unit,
    onBindingException: (Exception) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context ->
                val previewView = PreviewView(context)
                val preview = CameraPreview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)
                val selector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
                val imageAnalysis = ImageAnalysis.Builder().setTargetResolution(
                    Size(
                        previewView.width,
                        previewView.height
                    )
                ).setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST).build()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(context), onNewFrame)
                try {
                    cameraProviderFuture.get().bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                } catch (e: Exception) {
                    onBindingException(e)
                }
                previewView
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Preview(name = "Light mode")
@Preview(name = "Dark mode", uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun BarcodeScannerScreenPreview() {
    WooThemeWithBackground {
        BarcodeScanner(
            onNewFrame = {},
            onBindingException = {}
        )
    }
}
