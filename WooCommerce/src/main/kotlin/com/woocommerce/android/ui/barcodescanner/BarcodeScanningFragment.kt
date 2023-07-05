package com.woocommerce.android.ui.barcodescanner

import android.Manifest
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateBackWithResult
import com.woocommerce.android.extensions.openAppSettings
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel.PermissionState
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.orders.creation.GoogleMLKitCodeScanner
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BarcodeScanningFragment : BaseFragment() {
    private val viewModel: BarcodeScanningViewModel by viewModels()

    @Inject
    lateinit var codeScanner: GoogleMLKitCodeScanner
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    val (permissionState, cameraPermissionLauncher) = observeCameraPermissionState()
                    LaunchedEffect(key1 = Unit) {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    HandleCameraPermissionState(permissionState)
                    ObserveViewModelEvents(cameraPermissionLauncher)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
    @Composable
    private fun observeCameraPermissionState():
        Pair<PermissionState, ManagedActivityResultLauncher<String, Boolean>> {
        val permissionState: PermissionState by viewModel.permissionState.observeAsState(
            PermissionState.Unknown
        )
        val cameraPermissionLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.RequestPermission(),
            onResult = { granted ->
                viewModel.updatePermissionState(
                    granted,
                    shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)
                )
            }
        )
        return Pair(permissionState, cameraPermissionLauncher)
    }

    @Composable
    private fun HandleCameraPermissionState(
        permissionState: PermissionState,
    ) {
        when (permissionState) {
            PermissionState.Granted -> {
                BarcodeScanner()
            }

            is PermissionState.ShouldShowRationale -> {
                DisplayAlertDialog(
                    title = stringResource(id = permissionState.title),
                    message = stringResource(id = permissionState.message),
                    ctaLabel = stringResource(id = permissionState.ctaLabel),
                    ctaAction = { permissionState.ctaAction.invoke() }
                )
            }

            is PermissionState.PermanentlyDenied -> {
                DisplayAlertDialog(
                    title = stringResource(id = permissionState.title),
                    message = stringResource(id = permissionState.message),
                    ctaLabel = stringResource(id = permissionState.ctaLabel),
                    ctaAction = { permissionState.ctaAction.invoke() }
                )
            }

            PermissionState.Unknown -> {
                // no-op
            }
        }
    }

    @Composable
    private fun ObserveViewModelEvents(cameraPermissionLauncher: ManagedActivityResultLauncher<String, Boolean>) {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission -> {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                }
                BarcodeScanningViewModel.ScanningEvents.OpenAppSettings -> {
                    openAppSettings()
                }
            }
        }
    }

    @Composable
    private fun BarcodeScanner() {
        BarcodeScannerScreen(codeScanner) { codeScannerStatus ->
            lifecycleScope.launch {
                viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.CREATED) {
                    codeScannerStatus.collect { status ->
                        navigateBackWithResult(KEY_BARCODE_SCANNING_SCAN_STATUS, status)
                    }
                }
            }
        }
    }

    @Composable
    private fun DisplayAlertDialog(
        title: String,
        message: String,
        ctaLabel: String,
        ctaAction: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = { findNavController().navigateUp() },
            title = {
                Text(title)
            },
            text = {
                Text(message)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        ctaAction()
                    }
                ) {
                    Text(
                        ctaLabel,
                        color = MaterialTheme.colors.secondary,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            },
        )
    }

    override fun getFragmentTitle() = getString(R.string.barcode_scanning_title)

    companion object {
        const val KEY_BARCODE_SCANNING_SCAN_STATUS = "barcode_scanning_scan_status"
    }
}
