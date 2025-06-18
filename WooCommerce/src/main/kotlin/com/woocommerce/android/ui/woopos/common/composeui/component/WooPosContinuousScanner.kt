package com.woocommerce.android.ui.woopos.common.composeui.component

import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Suppress("TooGenericExceptionCaught")
@Composable
fun WooPosContinuousScanner(
    isEnabled: Boolean,
    onBarcodeDetected: (String) -> Unit,
    onBindingException: (Exception) -> Unit,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val barcodeProcessor: WooPosBarcodeProcessor = hiltViewModel()
    val coroutineScope = rememberCoroutineScope()

    val cameraProviderFuture = remember {
        ProcessCameraProvider.getInstance(context)
    }

    LaunchedEffect(isEnabled) {
        if (isEnabled) {
            barcodeProcessor.barcodeResults.collectLatest { barcode ->
                onBarcodeDetected(barcode)
            }
        }
    }

    val selector = remember {
        val cameraProvider = cameraProviderFuture.get()
        val hasBackCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
        val hasFrontCamera = cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)

        when {
            hasBackCamera -> CameraSelector.DEFAULT_BACK_CAMERA
            hasFrontCamera -> CameraSelector.DEFAULT_FRONT_CAMERA
            else -> error(IllegalStateException("No available camera"))
        }
    }

    DisposableEffect(lifecycleOwner, isEnabled) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && isEnabled) {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val imageAnalysisUseCase = ImageAnalysis.Builder()
                        .setTargetResolution(Size(1280, 720))
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
                        imageAnalysisUseCase
                    )
                } catch (e: Exception) {
                    onBindingException(e)
                }
            } else if (event == Lifecycle.Event.ON_PAUSE || !isEnabled) {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    cameraProvider.unbindAll()
                } catch (e: Exception) {
                    onBindingException(e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            try {
                val cameraProvider = cameraProviderFuture.get()
                cameraProvider.unbindAll()
            } catch (e: Exception) {
                onBindingException(e)
            }
        }
    }
}
