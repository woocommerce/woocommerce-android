package com.woocommerce.android.ui.barcodescanner

import android.content.res.Configuration
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.SurfaceOrientedMeteringPointFactory
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import java.util.concurrent.TimeUnit
import androidx.camera.core.Preview as CameraPreview

@Suppress("TooGenericExceptionCaught")
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
    val previewView = remember { PreviewView(context) }
    val cameraPreview = remember {
        CameraPreview.Builder().build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
    }
    val selector = remember {
        CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build()
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                try {
                    val cameraProvider = cameraProviderFuture.get()
                    val imageAnalysisUseCase = ImageAnalysis.Builder()
                        .setTargetResolution(
                            Size(
                                previewView.width,
                                previewView.height
                            )
                        ).setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .apply {
                            setAnalyzer(ContextCompat.getMainExecutor(context), onNewFrame)
                        }

                    cameraProvider.unbindAll()
                    val camera = cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        selector,
                        cameraPreview,
                        imageAnalysisUseCase
                    )

                    val factory = SurfaceOrientedMeteringPointFactory(
                        previewView.width.toFloat(),
                        previewView.height.toFloat()
                    )
                    val centerPoint = factory.createPoint(
                        previewView.width.toFloat() / 2,
                        previewView.height.toFloat() / 2
                    )
                    val action = FocusMeteringAction.Builder(centerPoint, FocusMeteringAction.FLAG_AF).apply {
                        // Confusing naming - that means focus and metering will reset after 2 seconds
                        setAutoCancelDuration(2, TimeUnit.SECONDS)
                    }.build()
                    camera.cameraControl.startFocusAndMetering(action)
                } catch (e: Exception) {
                    onBindingException(e)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Box {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )
        ScannerOverlay()
    }
}

@Composable
private fun ScannerOverlay() {
    Column {
        Box(
            modifier = Modifier
                .weight(0.24F)
                .fillMaxWidth()
                .background(colorResource(id = R.color.color_scrim_background))
        )
        Spacer(modifier = Modifier.weight(0.52F))
        Box(
            modifier = Modifier
                .weight(0.24F)
                .fillMaxWidth()
                .background(colorResource(id = R.color.color_scrim_background)),
            contentAlignment = Alignment.TopCenter
        ) {
            Text(
                modifier = Modifier.padding(dimensionResource(id = R.dimen.major_100)),
                text = stringResource(R.string.barcode_scanning_scan_product_barcode_label)
            )
        }
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
