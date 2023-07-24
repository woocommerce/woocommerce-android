package com.woocommerce.android.ui.orders.creation

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import coil.compose.AsyncImage
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class BarcodeScanningFragment : BaseFragment(R.layout.fragment_barcode_scanning) {
    private val viewModel: BarcodeScanningViewModel by viewModels()
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    @OptIn(ExperimentalMaterialApi::class)
    @Composable
    private fun barcodeScannerView() {
        val modalSheetState = rememberModalBottomSheetState(
            initialValue = ModalBottomSheetValue.Expanded,
            confirmStateChange = { it != ModalBottomSheetValue.HalfExpanded }
        )
        val scope = rememberCoroutineScope()
//        var showBottomSheet by remember { mutableStateOf(false) }

        var code: MutableState<String> = remember {
            mutableStateOf("")
        }
        val context = LocalContext.current
        val lifecycleOwner = LocalLifecycleOwner.current
        val cameraProviderFuture = remember {
            ProcessCameraProvider.getInstance(context)
        }
        var hasCamPermission by remember {
            mutableStateOf(
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_GRANTED
            )
        }
        val launcher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                hasCamPermission = granted
            }
        )
        LaunchedEffect(key1 = true) {
            launcher.launch(Manifest.permission.CAMERA)
        }

//        val isAddToCartBtnEnabled: MutableState<Boolean> = remember {
//            mutableStateOf(false)
//        }

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            if (hasCamPermission) {
                val barcodeScanner: BarcodeScanner = BarcodeScanning.getClient()
                AndroidView(
                    factory = { context ->
                        val previewView = PreviewView(context)
                        val preview = Preview.Builder().build()
                        val selector = CameraSelector.Builder()
                            .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                            .build()
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(
                                Size(
                                    previewView.width,
                                    previewView.height
                                )
                            )
                            .setBackpressureStrategy(STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(context),
                            ImageAnalysis.Analyzer { imageProxy ->
                                processImageProxy(barcodeScanner, imageProxy) {
                                    code.value = it
                                    viewModel.fetchProductBySKU(it)
                                }
                            }
                        )
                        try {
                            cameraProviderFuture.get().bindToLifecycle(
                                lifecycleOwner,
                                selector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                        previewView
                    },
                    modifier = Modifier.weight(1f)
                )

                val state = (viewModel.viewState.value as ViewState)

                LaunchedEffect(key1 = state.currentScannedProduct?.name) {
                    scope.launch {
                        modalSheetState.show()
                    }
                }
                if (state.currentScannedProduct?.name != null) {
                    var stockQuantity = remember {
                        mutableStateOf(state.currentScannedProduct.stockQuantity)
                    }
                    ModalBottomSheetLayout(
                        sheetState = modalSheetState,
                        sheetContent = {
                            Box(Modifier.defaultMinSize(minHeight = 1.dp))
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 100.dp)
                            ) {

                                Row(modifier = Modifier.fillMaxWidth()) {
                                    AsyncImage(
                                        model = state.currentScannedProduct.firstImageUrl,
                                        contentDescription = "Product Image",
                                        modifier = Modifier.padding(16.dp)
                                    )
                                    Text(
                                        text = "${(viewModel.viewState.value as ViewState).currentScannedProduct?.name}",
                                        modifier = Modifier
                                            .padding(16.dp)
                                    )
                                }
                                Text(
                                    text = "${(viewModel.viewState.value as ViewState).currentScannedProduct?.sku}",
                                    modifier = Modifier
                                        .align(Alignment.CenterHorizontally)
                                        .padding(16.dp)
                                )

                                Row(
                                    modifier = Modifier.padding(48.dp)
                                ) {
                                    Button(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
                                            .padding(16.dp),
                                        onClick = { stockQuantity.value-- }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Minus",
                                        )
                                    }

                                    Text(
                                        text = "" + stockQuantity.value,
                                        modifier = Modifier.padding(16.dp)
                                    )

                                    Button(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(15.dp, 15.dp, 0.dp, 0.dp))
                                            .padding(16.dp),
                                        onClick = { stockQuantity.value++ }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add",
                                        )
                                    }
                                }

                                Button(
                                    modifier = Modifier
                                        .padding(16.dp)
                                        .align(Alignment.CenterHorizontally),
                                    onClick = {
                                        viewModel.updateProduct(stockQuantity.value.toInt())
                                    }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        if ((viewModel.viewState.value as ViewState).showLoading) {
                                            CircularProgressIndicator(
                                                color = Color.White
                                            )
                                        } else {
                                            Text("Submit")
                                        }
                                    }
                                }
                            }
                        },
                        content = {
                            Column {
                                Text("Hello")
                            }
                        },
                    )
                }


//                (viewModel.viewState.value as ViewState).currentScannedProduct?.name?.let {
//                    isAddToCartBtnEnabled.value = true
//                } ?: run {
//                    isAddToCartBtnEnabled.value = false
//                }
//                Text(
//                    text = (viewModel.viewState.value as ViewState).currentScannedProduct?.name ?: "invalid SKU",
//                    fontSize = 20.sp,
//                    fontWeight = FontWeight.Bold,
//                    modifier = Modifier
//                        .fillMaxWidth()
//                        .padding(32.dp)
//                )
//                Row(
//                    modifier =Modifier.fillMaxWidth(),
//                    horizontalArrangement = Arrangement.spacedBy(24.dp)
//                ) {
//                    Button(
//                        onClick = {
//                            viewModel.addToCartClick()
//                        },
//                        enabled = true//isAddToCartBtnEnabled.value
//                    ) {
//                        Text(
//                            text = "Add to cart",
//                            fontSize = 20.sp,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier
//                                .padding(8.dp),
//                        )
//                    }
//
//                    Button(
//                        onClick = {
//                            viewModel.onDoneClick()
//                        }
//                    ) {
//                        Text(
//                            text = "Done",
//                            fontSize = 20.sp,
//                            fontWeight = FontWeight.Bold,
//                            modifier = Modifier
//                                .padding(8.dp)
//                        )
//                    }
//                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.viewState.observe(viewLifecycleOwner) {
            (requireView() as ComposeView).apply {
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WooThemeWithBackground {
                        barcodeScannerView()
                    }
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is BarcodeScanningViewModel.ScannedItems -> {
                    navigateBackWithResult(
                        key = "barcode",
                        result = it.selectedItems
                    )
                }
            }
        }
    }

    @SuppressLint("UnsafeExperimentalUsageError")
    private fun processImageProxy(
        barcodeScanner: BarcodeScanner,
        imageProxy: ImageProxy,
        onBarcodeCodeScanned: (String) -> Unit
    ) {
        val inputImage =
            InputImage.fromMediaImage(imageProxy.image!!, imageProxy.imageInfo.rotationDegrees)

        barcodeScanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                barcodes.forEach {
//                    textView?.text = "Add this item to cart: ${it.rawValue}"
                    onBarcodeCodeScanned(it.rawValue ?: "")
//                    addToCartView?.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                Log.e("ABCD", it.message ?: it.toString())
            }.addOnCompleteListener {
                // When the image is from CameraX analysis use case, must call image.close() on received
                // images when finished using them. Otherwise, new images may not be received or the camera
                // may stall.
                imageProxy.close()
            }
    }
}
