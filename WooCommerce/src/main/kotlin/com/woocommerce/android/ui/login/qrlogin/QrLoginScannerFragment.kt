package com.woocommerce.android.ui.login.qrlogin

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Hosts the QR scanner for the QR-first login flow. Hands parsed payloads to the
 * hosting [com.woocommerce.android.ui.login.LoginActivity] via [Listener].
 */
@AndroidEntryPoint
class QrLoginScannerFragment : androidx.fragment.app.Fragment() {
    companion object {
        const val TAG = "qr-login-scanner-fragment"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    interface Listener {
        fun onQrLoginScanned(payload: QrLoginPayload)
    }

    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val qrLoginViewModel: QrLoginScannerViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val permissionState = scannerViewModel.permissionState.observeAsState(
                initial = BarcodeScanningViewModel.PermissionState.Unknown
            )
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
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeScannerEvents()
        observeQrLoginEvents()
    }

    override fun onResume() {
        super.onResume()
        scannerViewModel.startCodesRecognition()
    }

    override fun onPause() {
        scannerViewModel.stopCodesRecognition()
        super.onPause()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun observeScannerEvents() {
        scannerViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is BarcodeScanningViewModel.ScanningEvents.LaunchCameraPermission ->
                    event.cameraLauncher.launch(KEY_CAMERA_PERMISSION)

                is BarcodeScanningViewModel.ScanningEvents.OpenAppSettings ->
                    WooPermissionUtils.showAppSettings(requireContext(), false)

                is BarcodeScanningViewModel.ScanningEvents.OnScanningResult ->
                    qrLoginViewModel.onScanResult(event.status)

                is Exit -> requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }
    }

    private fun observeQrLoginEvents() {
        qrLoginViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is QrLoginScannerViewModel.Dispatch.SiteAppPassword ->
                    listener?.onQrLoginScanned(event.payload)

                is QrLoginScannerViewModel.Dispatch.WpComToken ->
                    listener?.onQrLoginScanned(event.payload)

                is QrLoginScannerViewModel.Dispatch.UrlOnly ->
                    listener?.onQrLoginScanned(event.payload)

                QrLoginScannerViewModel.Dispatch.InvalidPayload ->
                    uiMessageResolver.showSnack(R.string.login_qr_scanner_error_payload)

                QrLoginScannerViewModel.Dispatch.ScannerFailure ->
                    uiMessageResolver.showSnack(R.string.login_qr_scanner_error_generic)
            }
        }
    }
}
