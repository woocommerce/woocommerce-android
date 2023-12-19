package com.woocommerce.android.ui.products.inventory

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState.Unknown
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.main.MainNavigationRouter
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ScanToUpdateInventoryBarcodeScannerFragment : BaseFragment() {
    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val viewModel: ScanToUpdateInventoryViewModel by viewModels()
    @Inject
    lateinit var uiMessageResolver: UIMessageResolver
    private var undoSnackbar: Snackbar? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            ScanToUpdateInventoryScreen(
                onNewFrame = scannerViewModel::onNewFrame,
                onBindingException = scannerViewModel::onBindingException,
                permissionState = scannerViewModel.permissionState.observeAsState(Unknown),
                onCameraPermissionResult = { granted ->
                    scannerViewModel.updatePermissionState(
                        granted,
                        shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                    )
                },
                viewState = viewModel.viewState.collectAsState(),
                onBottomSheetDismissed = viewModel::onBottomSheetDismissed,
                onIncrementQuantityClicked = viewModel::onIncrementQuantityClicked,
                onUpdateQuantityClicked = viewModel::onUpdateQuantityClicked,
                onViewProductDetailsClicked = viewModel::onViewProductDetailsClicked,
                onManualQuantityEntered = viewModel::onManualQuantityEntered,
            )
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
        undoSnackbar?.dismiss()
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
        viewModel.event.observe(viewLifecycleOwner) {
            when (it) {
                is MultiLiveEvent.Event.ShowUiStringSnackbar -> {
                    uiMessageResolver.showSnack(it.message)
                }
                is MultiLiveEvent.Event.ShowUndoSnackbar -> {
                    showUndoSnackbar(
                        message = it.message,
                        actionListener = it.undoAction,
                        dismissCallback = it.dismissAction
                    )
                }
                is ScanToUpdateInventoryViewModel.NavigateToProductDetailsEvent -> {
                    (requireActivity() as? MainNavigationRouter)?.showProductDetail(it.productId)
                }
            }
        }
    }

    private fun showUndoSnackbar(
        message: String,
        actionListener: View.OnClickListener,
        dismissCallback: Snackbar.Callback
    ) {
        undoSnackbar = uiMessageResolver.getUndoSnack(
            message = message,
            actionListener = actionListener,
        ).also {
            it.addCallback(dismissCallback)
            it.show()
        }
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }
}
