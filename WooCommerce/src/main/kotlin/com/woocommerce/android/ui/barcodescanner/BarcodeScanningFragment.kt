package com.woocommerce.android.ui.barcodescanner

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.CodeScannerStatus
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class BarcodeScanningFragment : BaseFragment() {
    private val viewModel: BarcodeScanningViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view as ComposeView
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        observeCameraPermissionState(view)
        observeViewModelEvents()
    }

    override fun onResume() {
        super.onResume()
        viewModel.startCodesRecognition()
    }

    override fun onPause() {
        viewModel.stopCodesRecognition()
        super.onPause()
    }

    private fun observeCameraPermissionState(view: ComposeView) {
        viewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            view.setContent {
                WooThemeWithBackground {
                    BarcodeScannerScreen(
                        onNewFrame = viewModel::onNewFrame,
                        onBindingException = viewModel::onBindingException,
                        permissionState = permissionState,
                        onResult = { granted ->
                            viewModel.updatePermissionState(
                                granted,
                                shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                            )
                        }
                    )
                }
            }
        }
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

                is BarcodeScanningViewModel.ScanningEvents.OnScanningResult -> {
                    navigateToNextScreen(event.status)
                }

                is Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
    }

    private fun navigateToNextScreen(status: CodeScannerStatus) {
        navigateBackWithResult(KEY_BARCODE_SCANNING_SCAN_STATUS, status)
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
