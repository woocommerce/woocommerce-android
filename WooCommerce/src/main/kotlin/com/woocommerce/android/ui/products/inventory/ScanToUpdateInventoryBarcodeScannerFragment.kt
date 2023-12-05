package com.woocommerce.android.ui.products.inventory

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.dimensionResource
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.Unknown
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.products.inventory.ScanToUpdateInventoryViewModel.ViewState.ProductLoaded
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.filter

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BaseFragment() {
    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val viewModel: ScanToUpdateInventoryViewModel by viewModels()

    @OptIn(ExperimentalMaterialApi::class)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                val sheetState = rememberModalBottomSheetState(
                    initialValue = ModalBottomSheetValue.Hidden,
                    skipHalfExpanded = true
                )
                ModalBottomSheetLayout(
                    sheetState = sheetState,
                    content = {
                        BarcodeScannerScreen(
                            onNewFrame = scannerViewModel::onNewFrame,
                            onBindingException = scannerViewModel::onBindingException,
                            permissionState = scannerViewModel.permissionState.observeAsState(Unknown),
                            onResult = { granted ->
                                scannerViewModel.updatePermissionState(
                                    granted,
                                    shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                                )
                            }
                        )
                    },
                    sheetShape = RoundedCornerShape(
                        topStart = dimensionResource(id = R.dimen.corner_radius_large),
                        topEnd = dimensionResource(id = R.dimen.corner_radius_large)
                    ),
                    sheetContent = {
                        viewModel.viewState.collectAsState().value.let { state ->
                            if (state is ProductLoaded) {
                                QuickInventoryUpdateBottomSheet(
                                    state = state,
                                    onIncrementQuantityClicked = viewModel::onIncrementQuantityClicked
                                )
                            }
                            LaunchedEffect(state) {
                                if (state is ProductLoaded) {
                                    sheetState.show()
                                } else {
                                    sheetState.hide()
                                }
                            }
                            LaunchedEffect(sheetState) {
                                snapshotFlow { sheetState.currentValue }
                                    .filter { it == ModalBottomSheetValue.Hidden }
                                    .collect {
                                        viewModel.onBottomSheetDismissed()
                                    }
                            }
                        }
                    }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
