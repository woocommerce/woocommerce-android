package com.woocommerce.android.ui.login.qrlogin

import android.Manifest
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.WooLog
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import org.wordpress.android.login.LoginListener

/**
 * Hosts [QrLoginScannerScreen] for the QR-first login flow. Plumbs camera permissions, the
 * deep-link payload, and the post-login handoff back to the activity; the conditional UI lives
 * in the screen composable.
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

    // Captured once at fragment creation so subsequent reads survive `arguments?.remove(...)`.
    // Stays false on process-death recovery so the user falls back to the scanner.
    private var isDeepLinkEntry: Boolean = false

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    private var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        isDeepLinkEntry = savedInstanceState == null && deepLinkPayload != null
    }

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
        val currentError by qrLoginViewModel.currentError.collectAsStateWithLifecycle()
        QrLoginScannerScreen(
            permissionState = permissionState,
            authenticating = authenticating,
            endpointMissing = endpointMissing,
            pendingConfirmation = pendingConfirmation,
            currentError = currentError,
            showCamera = !isDeepLinkEntry,
            onNewFrame = scannerViewModel::onNewFrame,
            onBindingException = scannerViewModel::onBindingException,
            onPermissionResult = { granted ->
                scannerViewModel.updatePermissionState(
                    granted,
                    shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                )
            },
            onConfirmSite = qrLoginViewModel::onConfirmSite,
            onCancelSite = ::handleCancelSite,
            onStartOver = ::handleStartOver,
            onFallbackClicked = { listener?.onQrLoginFallbackClicked() },
        )
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
        if (!isDeepLinkEntry) {
            scannerViewModel.startCodesRecognition()
        }
        unifiedLoginTracker.setFlowAndStep(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
    }

    override fun onPause() {
        if (!isDeepLinkEntry) {
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
                is QrLoginScannerViewModel.Dispatch.LoggedIn -> handleLoggedIn(event.localSiteId)
            }
        }
    }

    /**
     * In camera mode, dismissing confirm just clears the overlay and the camera resumes.
     * In deep-link mode there is no camera underneath, so the same dismissal would leave the
     * user on a blank surface — exit the fragment instead.
     */
    private fun handleCancelSite() {
        qrLoginViewModel.onCancelSite()
        if (isDeepLinkEntry) requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /**
     * Same reasoning as [handleCancelSite]: error/endpoint-missing screens in deep-link mode
     * have nothing to fall back to once dismissed, so we exit. In camera mode the scanner
     * resumes underneath as soon as the overlay is cleared.
     */
    private fun handleStartOver() {
        qrLoginViewModel.onStartOver()
        if (isDeepLinkEntry) requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun handleLoggedIn(localSiteId: Int) {
        // Stop the camera before handing off so the preview doesn't leak.
        scannerViewModel.stopCodesRecognition()
        val activeListener = listener
        if (activeListener != null) {
            activeListener.onQrLoginCompleted(localSiteId)
            return
        }
        val activity = requireActivity()
        val loginListener = activity as? LoginListener
        if (loginListener == null) {
            WooLog.e(
                WooLog.T.LOGIN,
                "QR login finished but ${activity.javaClass.simpleName} is not a Listener or LoginListener"
            )
            return
        }
        loginListener.loggedInViaUsernamePassword(arrayListOf(localSiteId))
    }
}
