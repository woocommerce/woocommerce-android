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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.DialogProperties
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.R
import com.woocommerce.android.ui.barcodescanner.BarcodeScannerScreen
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.component.ProgressDialog
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.WooLog
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
        private const val ARG_DEEP_LINK_PAYLOAD = "arg-deep-link-payload"

        /**
         * Creates an instance that skips the camera preview and feeds [rawPayload] (a
         * `woocommerce://qr-login?...` URI) straight into the login pipeline — used when the
         * user opens the deep link from a browser.
         */
        fun forDeepLink(rawPayload: String): QrLoginScannerFragment = QrLoginScannerFragment().apply {
            arguments = Bundle().apply { putString(ARG_DEEP_LINK_PAYLOAD, rawPayload) }
        }
    }

    interface Listener {
        fun onQrLoginCompleted(localSiteId: Int)
        fun onQrLoginFallbackClicked()
    }

    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val qrLoginViewModel: QrLoginScannerViewModel by viewModels()

    private val deepLinkPayload: String?
        get() = arguments?.getString(ARG_DEEP_LINK_PAYLOAD)

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeView {
        val permissionState = scannerViewModel.permissionState.observeAsState(
            initial = BarcodeScanningViewModel.PermissionState.Unknown
        )
        val authenticating by qrLoginViewModel.isAuthenticating.collectAsStateWithLifecycle()
        val endpointMissing by qrLoginViewModel.endpointMissing.collectAsStateWithLifecycle()
        val pendingConfirmation by qrLoginViewModel.pendingConfirmation.collectAsStateWithLifecycle()
        Box(modifier = Modifier.fillMaxSize()) {
            if (endpointMissing) {
                QrLoginEndpointMissingScreen(
                    onEnterUrlClicked = { listener?.onQrLoginFallbackClicked() },
                    onRetryClicked = { qrLoginViewModel.onRetryAfterBlockingError() }
                )
            } else {
                if (deepLinkPayload == null) {
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
                }
                if (authenticating) {
                    ProgressDialog(
                        title = "",
                        subtitle = stringResource(id = R.string.login_qr_scanner_authenticating),
                        properties = DialogProperties(
                            dismissOnBackPress = false,
                            dismissOnClickOutside = false
                        )
                    )
                }
                pendingConfirmation?.let { pending ->
                    QrLoginConfirmSiteDialog(
                        host = pending.host,
                        onConfirm = qrLoginViewModel::onConfirmSite,
                        onCancel = qrLoginViewModel::onCancelSite
                    )
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeScannerEvents()
        observeQrLoginEvents()
        if (savedInstanceState == null) {
            unifiedLoginTracker.track(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
            deepLinkPayload?.let { payload ->
                qrLoginViewModel.onDeepLinkPayload(payload)
                // Drop the raw deep link from fragment arguments so it isn't parcelled into
                // saved-instance-state / recents, and so a state-loss recovery falls back to the
                // scanner instead of replaying a single-use token.
                arguments?.remove(ARG_DEEP_LINK_PAYLOAD)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (deepLinkPayload == null) {
            scannerViewModel.startCodesRecognition()
        }
        unifiedLoginTracker.setFlowAndStep(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
    }

    override fun onPause() {
        if (deepLinkPayload == null) {
            scannerViewModel.stopCodesRecognition()
        }
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
                    val activeListener = listener
                    if (activeListener != null) {
                        activeListener.onQrLoginCompleted(event.localSiteId)
                    } else {
                        val activity = requireActivity()
                        val loginListener = activity as? LoginListener
                        if (loginListener == null) {
                            WooLog.e(
                                WooLog.T.LOGIN,
                                "QR login finished but ${activity.javaClass.simpleName} is not a Listener or LoginListener"
                            )
                        } else {
                            loginListener.loggedInViaUsernamePassword(arrayListOf(event.localSiteId))
                        }
                    }
                }

                is QrLoginScannerViewModel.Dispatch.RecoverableError ->
                    uiMessageResolver.showSnack(event.reason.toMessage())
            }
        }
    }

    @StringRes
    private fun QrLoginScannerViewModel.ErrorReason.toMessage(): Int = when (this) {
        QrLoginScannerViewModel.ErrorReason.InvalidPayload -> R.string.login_qr_scanner_error_payload
        QrLoginScannerViewModel.ErrorReason.Scanner -> R.string.login_qr_scanner_error_generic
        QrLoginScannerViewModel.ErrorReason.TokenRejected -> R.string.login_qr_scanner_error_token
        QrLoginScannerViewModel.ErrorReason.EndpointMissing -> R.string.login_qr_scanner_error_endpoint
        QrLoginScannerViewModel.ErrorReason.RateLimited -> R.string.login_qr_scanner_error_rate_limited
        QrLoginScannerViewModel.ErrorReason.Network -> R.string.login_qr_scanner_error_network
        QrLoginScannerViewModel.ErrorReason.ServerError -> R.string.login_qr_scanner_error_server
        QrLoginScannerViewModel.ErrorReason.SiteAuthFailure -> R.string.login_qr_scanner_error_site_auth
        QrLoginScannerViewModel.ErrorReason.NotAWooSite -> R.string.login_qr_scanner_error_not_woo
        QrLoginScannerViewModel.ErrorReason.UserNotEligible -> R.string.login_qr_scanner_error_user_role
        QrLoginScannerViewModel.ErrorReason.Unknown -> R.string.login_qr_scanner_error_generic
    }
}
