@file:Suppress("ImportOrdering")

package com.woocommerce.android.ui.woopos.common.composeui.component

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import com.woocommerce.android.ui.woopos.common.composeui.designsystem.WooPosCornerRadius
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.launch
import androidx.camera.core.Preview as CameraPreview

@Suppress("TooGenericExceptionCaught")
@Composable
fun WooPosCameraPreview(
    isEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onBindingException: (Exception) -> Unit,
    modifier: Modifier = Modifier
) {
    if (!isEnabled) return

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeProcessor: WooPosBarcodeProcessor = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }
    val previewView = remember { PreviewView(context) }
    val cameraPreview = remember {
        CameraPreview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    }

    val selector = buildCameraSelector(cameraProviderFuture)

    CameraLifecycleHandler(
        lifecycleOwner = lifecycleOwner,
        isEnabled = isEnabled,
        cameraProviderFuture = cameraProviderFuture,
        previewView = previewView,
        selector = selector,
        cameraPreview = cameraPreview,
        barcodeProcessor = barcodeProcessor,
        coroutineScope = coroutineScope,
        context = context,
        onBindingException = onBindingException
    )

    BarcodeResultsCollector(
        isEnabled = isEnabled,
        barcodeProcessor = barcodeProcessor,
        coroutineScope = coroutineScope,
        onBarcodeDetected = onBarcodeDetected
    )

    Box(
        modifier = modifier
            .size(120.dp)
            .clip(RoundedCornerShape(WooPosCornerRadius.Small.value))
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(WooPosCornerRadius.Small.value)
            )
    ) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.size(120.dp)
        )
    }
}

@Composable
private fun buildCameraSelector(
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>
): CameraSelector {
    return remember {
        val cameraProvider = cameraProviderFuture.get()
        val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

        when {
            hasBackCamera -> CameraSelector.DEFAULT_BACK_CAMERA
            hasFrontCamera -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> error(IllegalStateException("No available camera"))
        }
    }
}

@Suppress("TooGenericExceptionCaught", "LongParameterList")
@Composable
private fun CameraLifecycleHandler(
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isEnabled: Boolean,
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    selector: CameraSelector,
    cameraPreview: androidx.camera.core.Preview,
    barcodeProcessor: WooPosBarcodeProcessor,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    onBindingException: (Exception) -> Unit
) {
    DisposableEffect(lifecycleOwner, isEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> {
                    if (isEnabled) {
                        startCamera(
                            cameraProviderFuture,
                            previewView,
                            selector,
                            cameraPreview,
                            barcodeProcessor,
                            coroutineScope,
                            context,
                            lifecycleOwner,
                            isEnabled,
                            onBindingException
                        )
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    stopCamera(cameraProviderFuture, onBindingException)
                }
                else -> { /* no-op */ }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            stopCamera(cameraProviderFuture, onBindingException)
        }
    }
}

@Suppress("TooGenericExceptionCaught", "LongParameterList")
private fun startCamera(
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>,
    previewView: PreviewView,
    selector: CameraSelector,
    cameraPreview: androidx.camera.core.Preview,
    barcodeProcessor: WooPosBarcodeProcessor,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    context: android.content.Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    isEnabled: Boolean,
    onBindingException: (Exception) -> Unit
) {
    previewView.post {
        try {
            val cameraProvider = cameraProviderFuture.get()
            val imageAnalysisUseCase = ImageAnalysis.Builder()
                .setTargetResolution(Size(CAMERA_WIDTH, CAMERA_HEIGHT))
                .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .apply {
                    setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                        if (isEnabled) {
                            coroutineScope.launch {
                                barcodeProcessor.processImage(imageProxy)
                            }
                        } else {
                            imageProxy.close()
                        }
                    }
                }

            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                selector,
                cameraPreview,
                imageAnalysisUseCase
            )
        } catch (e: Exception) {
            onBindingException(e)
        }
    }
}

@Suppress("TooGenericExceptionCaught")
private fun stopCamera(
    cameraProviderFuture: com.google.common.util.concurrent.ListenableFuture<ProcessCameraProvider>,
    onBindingException: (Exception) -> Unit
) {
    try {
        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
    } catch (e: Exception) {
        onBindingException(e)
    }
}

@Composable
private fun BarcodeResultsCollector(
    isEnabled: Boolean,
    barcodeProcessor: WooPosBarcodeProcessor,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    onBarcodeDetected: (String) -> Unit
) {
    DisposableEffect(isEnabled) {
        if (isEnabled) {
            val job = coroutineScope.launch {
                barcodeProcessor.barcodeResults.collect { barcode ->
                    onBarcodeDetected(barcode)
                }
            }
            onDispose {
                job.cancel()
            }
        } else {
            onDispose { }
        }
    }
}

private const val CAMERA_WIDTH = 1280
private const val CAMERA_HEIGHT = 720
