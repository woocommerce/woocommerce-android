package com.woocommerce.android.ui.barcodescanner

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.CallSuper
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
open class BarcodeScanningFragment : BaseFragment() {
    private val viewModel: BarcodeScanningViewModel by viewModels()

    @Inject
    lateinit var codeScanner: GoogleMLKitCodeScanner

    open val isContinuousScanningEnabled: Boolean = false

    @CallSuper
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    @CallSuper
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view as ComposeView
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        observeCameraPermissionState(view)
        observeViewModelEvents()
    }

    private fun observeCameraPermissionState(view: ComposeView) {
        viewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            view.setContent {
                WooThemeWithBackground {
                    BarcodeScannerScreen(
                        codeScanner = codeScanner,
                        permissionState = permissionState,
                        onResult = { granted ->
                            viewModel.updatePermissionState(
                                granted,
                                shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                            )
                        },
                        onScannedResult = { codeScannerStatus ->
                            viewLifecycleOwner.lifecycleScope.launch {
                                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                                    codeScannerStatus.collect { status ->
                                        onScannedResult(status)
                                    }
                                }
                            }
                        },
                        isContinuousScanningEnabled = isContinuousScanningEnabled,
                        overlay = getScannerOverlay(),
                    )
                }
            }
        }
    }

    open fun getScannerOverlay(): @Composable BoxScope.() -> Unit = {}

    open fun onScannedResult(status: CodeScannerStatus) {
        navigateBackWithResult(KEY_BARCODE_SCANNING_SCAN_STATUS, status)
    }

    private fun observeViewModelEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission -> {
                    event.cameraLauncher.launch(KEY_CAMERA_PERMISSION)
                }

                is BarcodeScanningViewModel.ScanningEvents.OpenAppSettings -> {
                    WooPermissionUtils.showAppSettings(requireContext(), false)
                }

                is Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }
    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
