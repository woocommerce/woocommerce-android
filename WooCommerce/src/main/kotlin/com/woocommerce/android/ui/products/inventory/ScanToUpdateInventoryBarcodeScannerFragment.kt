package com.woocommerce.android.ui.products.inventory

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
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import com.woocommerce.android.viewmodel.fixedHiltNavGraphViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BaseFragment() {
    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val viewModel: ScanToUpdateInventoryViewModel by fixedHiltNavGraphViewModels(R.id.nav_graph_main)
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

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
        scannerViewModel.startCodesRecognition()
    }

    override fun onPause() {
        scannerViewModel.stopCodesRecognition()
        super.onPause()
    }

    private fun observeCameraPermissionState(view: ComposeView) {
        scannerViewModel.permissionState.observe(viewLifecycleOwner) { permissionState ->
            view.setContent {
                WooThemeWithBackground {
                    BarcodeScannerScreen(
                        onNewFrame = scannerViewModel::onNewFrame,
                        onBindingException = scannerViewModel::onBindingException,
                        permissionState = permissionState,
                        onResult = { granted ->
                            scannerViewModel.updatePermissionState(
                                granted,
                                shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                            )
                        }
                    )
                    QuickInventoryUpdateBottomSheet(
                        viewState = viewModel.viewState,
                        onDismiss = viewModel::onBottomSheetDismissed,
                        onIncrementQuantityClicked = viewModel::onIncrementQuantityClicked
                    )
                }
            }
        }
    }

    private fun observeViewModelEvents() {
        scannerViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission -> {
                    event.cameraLauncher.launch(KEY_CAMERA_PERMISSION)
                }

                is BarcodeScanningViewModel.ScanningEvents.OpenAppSettings -> {
                    WooPermissionUtils.showAppSettings(requireContext(), false)
                }

                is BarcodeScanningViewModel.ScanningEvents.OnScanningResult -> {
                    viewModel.onBarcodeScanningResult(event.status)
                }

                is MultiLiveEvent.Event.Exit -> {
                    findNavController().navigateUp()
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is MultiLiveEvent.Event.ShowUiStringSnackbar -> {
                    uiMessageResolver.showSnack(it.message)
                }
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
