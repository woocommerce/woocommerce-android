package com.woocommerce.android.ui.login.qrlogin

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.wordpress.android.login.LoginListener

/**
 * Hosts the QR scanner for the QR-first login flow. Drives the parse → exchange → login chain
 * via [QrLoginScannerViewModel] and tells the activity when the user is logged in via [Listener].
 */
@AndroidEntryPoint
class QrLoginScannerFragment : Fragment() {
    companion object {
        const val TAG = "qr-login-scanner-fragment"
        const val KEY_CAMERA_PERMISSION = Manifest.permission.CAMERA
    }

    interface Listener {
        fun onQrLoginCompleted(localSiteId: Int)
        fun onQrLoginFallbackClicked()
    }

    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val qrLoginViewModel: QrLoginScannerViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

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
            val authenticating by qrLoginViewModel.isAuthenticating.collectAsStateWithLifecycle()
            val endpointMissing by qrLoginViewModel.endpointMissing.collectAsStateWithLifecycle()
            val pendingConfirmation by qrLoginViewModel.pendingConfirmation.collectAsStateWithLifecycle()
            WooThemeWithBackground {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (endpointMissing) {
                        QrLoginEndpointMissingScreen(
                            onEnterUrlClicked = { listener?.onQrLoginFallbackClicked() },
                            onRetryClicked = { qrLoginViewModel.onRetryAfterBlockingError() }
                        )
                    } else {
                        BarcodeScannerScreen(
                            onNewFrame = scannerViewModel::onNewFrame,
                            onBindingException = scannerViewModel::onBindingException,
                            permissionState = permissionState,
                            onResult = { granted ->
                                scannerViewModel.updatePermissionState(
                                    granted,
                                    shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                                )
                            },
                            overlayLabel = R.string.login_qr_scanner_hint
                        )
                        if (authenticating) {
                            ProgressDialog(
                                title = "",
                                subtitle = stringResource(id = R.string.login_qr_scanner_authenticating)
                            )
                        }
                        pendingConfirmation?.let { pending ->
                            ConfirmSiteDialog(
                                host = pending.host,
                                onConfirm = qrLoginViewModel::onConfirmSite,
                                onCancel = qrLoginViewModel::onCancelSite
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeScannerEvents()
        observeQrLoginEvents()
        if (savedInstanceState == null) {
            unifiedLoginTracker.track(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
        }
    }

    override fun onResume() {
        super.onResume()
        scannerViewModel.startCodesRecognition()
        unifiedLoginTracker.setFlowAndStep(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
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
                is QrLoginScannerViewModel.Dispatch.LoggedIn -> {
                    // Stop the camera before handing off so the preview doesn't leak.
                    scannerViewModel.stopCodesRecognition()
                    listener?.onQrLoginCompleted(event.localSiteId)
                        ?: (requireActivity() as LoginListener)
                            .loggedInViaUsernamePassword(arrayListOf(event.localSiteId))
                }

                is QrLoginScannerViewModel.Dispatch.RecoverableError ->
                    uiMessageResolver.showSnack(event.reason.toMessage())
            }
        }
    }

    @Composable
    private fun ConfirmSiteDialog(
        host: String,
        onConfirm: () -> Unit,
        onCancel: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onCancel,
            title = { Text(text = stringResource(id = R.string.login_qr_confirm_title)) },
            text = { Text(text = stringResource(id = R.string.login_qr_confirm_body, host)) },
            confirmButton = {
                TextButton(onClick = onConfirm) {
                    Text(text = stringResource(id = R.string.login_qr_confirm_connect))
                }
            },
            dismissButton = {
                TextButton(onClick = onCancel) {
                    Text(text = stringResource(id = R.string.login_qr_confirm_cancel))
                }
            }
        )
    }

    @StringRes
    private fun QrLoginScannerViewModel.ErrorReason.toMessage(): Int = when (this) {
        QrLoginScannerViewModel.ErrorReason.InvalidPayload -> R.string.login_qr_scanner_error_payload
        QrLoginScannerViewModel.ErrorReason.Scanner -> R.string.login_qr_scanner_error_generic
        QrLoginScannerViewModel.ErrorReason.TokenRejected -> R.string.login_qr_scanner_error_token
        QrLoginScannerViewModel.ErrorReason.EndpointMissing -> R.string.login_qr_scanner_error_endpoint
        QrLoginScannerViewModel.ErrorReason.RateLimited -> R.string.login_qr_scanner_error_rate_limited
        QrLoginScannerViewModel.ErrorReason.Network -> R.string.login_qr_scanner_error_network
        QrLoginScannerViewModel.ErrorReason.SiteAuthFailure -> R.string.login_qr_scanner_error_site_auth
        QrLoginScannerViewModel.ErrorReason.NotAWooSite -> R.string.login_qr_scanner_error_not_woo
        QrLoginScannerViewModel.ErrorReason.UserNotEligible -> R.string.login_qr_scanner_error_user_role
        QrLoginScannerViewModel.ErrorReason.Unknown -> R.string.login_qr_scanner_error_generic
    }
}
