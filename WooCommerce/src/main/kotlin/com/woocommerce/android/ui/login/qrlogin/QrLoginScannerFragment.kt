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
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.barcodescanner.BarcodeScanningViewModel
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.util.WooPermissionUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

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
        private const val KEY_IS_DEEP_LINK_ENTRY = "key-is-deep-link-entry"

        /**
         * Creates an instance that skips the camera preview and feeds [rawPayload] (a
         * `woocommerce://qr-login?...` URI) straight into the login pipeline — used when the
         * user opens the deep link from a browser. The payload is held in [arguments] only
         * briefly: the fragment moves it into a transient field and clears [arguments] in
         * [onCreate] so the single-use token is never parcelled into fragment instance state.
         */
        fun forDeepLink(rawPayload: String): QrLoginScannerFragment = QrLoginScannerFragment().apply {
            arguments = Bundle().apply { putString(ARG_DEEP_LINK_PAYLOAD, rawPayload) }
        }
    }

    interface Listener {
        fun onQrLoginCompleted(localSiteId: Int)
        fun onQrLoginFallbackClicked()
        fun onQrLoginSiteUrlPrefill(siteUrl: String)
        fun onQrLoginAppLoginCredentials(siteUrl: String, username: String)
        fun onQrLoginAppLoginWpComEmail(siteUrl: String, wpComEmail: String)
        fun onQrLoginHelpClicked()
    }

    private val scannerViewModel: BarcodeScanningViewModel by viewModels()
    private val qrLoginViewModel: QrLoginScannerViewModel by viewModels()

    // The payload is captured once at fragment creation, kept in memory only, and consumed in
    // onViewCreated. The entry mode itself is safe to restore across rotation, but only while the
    // retained ViewModel still holds a non-idle overlay state. After process death the ViewModel
    // is idle again, so we fall back to the scanner instead of replaying a stale token.
    private var pendingDeepLinkPayload: String? = null
    private var isDeepLinkEntry: Boolean = false

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private var listener: Listener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val payload = arguments?.getString(ARG_DEEP_LINK_PAYLOAD)
        // Strip the payload from arguments before any save-state cycle can persist it.
        arguments = null
        isDeepLinkEntry = savedInstanceState?.getBoolean(KEY_IS_DEEP_LINK_ENTRY) == true &&
            qrLoginViewModel.uiState.value !is QrLoginScannerViewModel.UiState.Idle
        if (savedInstanceState == null && payload != null) {
            pendingDeepLinkPayload = payload
            isDeepLinkEntry = true
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(KEY_IS_DEEP_LINK_ENTRY, isDeepLinkEntry)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeView {
        val permissionState = scannerViewModel.permissionState.observeAsState(
            initial = BarcodeScanningViewModel.PermissionState.Unknown
        )
        val uiState by qrLoginViewModel.uiState.collectAsStateWithLifecycle()
        QrLoginScannerScreen(
            permissionState = permissionState,
            uiState = uiState,
            showCamera = !isDeepLinkEntry,
            onNewFrame = scannerViewModel::onNewFrame,
            onBindingException = scannerViewModel::onBindingException,
            onPermissionResult = { granted ->
                scannerViewModel.updatePermissionState(
                    granted,
                    shouldShowRequestPermissionRationale(KEY_CAMERA_PERMISSION)
                )
            },
            onCancelNumberMatch = ::handleCancelNumberMatch,
            onConfirmSessionReplace = qrLoginViewModel::onConfirmSessionReplace,
            onCancelSessionReplace = ::handleCancelSessionReplace,
            onStartOver = ::handleStartOver,
            onRetryExchange = qrLoginViewModel::onRetryExchange,
            onFallbackClicked = { listener?.onQrLoginFallbackClicked() },
            onHelpClicked = {
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_HELP_TAPPED)
                unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.SHOW_HELP)
                listener?.onQrLoginHelpClicked()
            },
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        observeScannerEvents()
        observeQrLoginEvents()
        if (savedInstanceState == null) {
            unifiedLoginTracker.track(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_SCAN)
            pendingDeepLinkPayload?.let { qrLoginViewModel.onDeepLinkPayload(it) }
            pendingDeepLinkPayload = null
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
                is QrLoginScannerViewModel.Dispatch.OpenWpComMagicLinkUrl ->
                    openWpComMagicLinkUrl(event.url)
                is QrLoginScannerViewModel.Dispatch.RouteToSiteAddressEntry ->
                    routeToSiteAddressEntry(event.siteUrl)
                is QrLoginScannerViewModel.Dispatch.RouteToAppLoginCredentials ->
                    routeToAppLoginCredentials(event.siteUrl, event.username)
                is QrLoginScannerViewModel.Dispatch.RouteToAppLoginWpComEmail ->
                    routeToAppLoginWpComEmail(event.siteUrl, event.wpComEmail)
            }
        }
    }

    /**
     * In camera mode, cancelling the number-match step clears the overlay and the camera
     * resumes underneath. In deep-link mode there is no camera, so the same dismissal would
     * leave the user on a blank surface — exit the fragment instead.
     */
    private fun handleCancelNumberMatch() {
        qrLoginViewModel.onCancelNumberMatch()
        if (isDeepLinkEntry) requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /**
     * In practice the warning is only reachable via deep link (the in-app scanner is gated
     * behind a logged-out [LoginActivity]), so cancelling closes [LoginActivity] and returns
     * the merchant to whatever they were doing before tapping the QR link. The existing session
     * is left intact.
     */
    private fun handleCancelSessionReplace() {
        qrLoginViewModel.onCancelSessionReplace()
        requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    /**
     * Same reasoning as [handleCancelNumberMatch]: error/endpoint-missing screens in deep-link
     * mode have nothing to fall back to once dismissed, so we exit. In camera mode the scanner
     * resumes underneath as soon as the overlay is cleared.
     */
    private fun handleStartOver() {
        qrLoginViewModel.onStartOver()
        if (isDeepLinkEntry) requireActivity().onBackPressedDispatcher.onBackPressed()
    }

    private fun handleLoggedIn(localSiteId: Int) {
        // Stop the camera before handing off so the preview doesn't leak.
        scannerViewModel.stopCodesRecognition()
        requireNotNull(listener) {
            "${requireActivity().javaClass.simpleName} must implement QrLoginScannerFragment.Listener"
        }.onQrLoginCompleted(localSiteId)
    }

    /**
     * Hand the wp.com magic-login URL off to the browser. wp.com 3xx-redirects to
     * `woocommerce://magic-login`, and the existing intent-filter on `MagicLinkInterceptActivity`
     * picks it up — same end-to-end path as a 3rd-party scanner.
     *
     * [ChromeCustomTabUtils.launchUrl] handles the no-browser edge case for us: it falls back to
     * a plain external launch and shows a toast on `ActivityNotFoundException`, so we don't need
     * a try/catch here. Custom Tabs forward non-http schemes (here, the redirect target
     * `woocommerce://magic-login`) to the OS as Intents, so the deeplink still fires.
     */
    private fun openWpComMagicLinkUrl(url: String) {
        scannerViewModel.stopCodesRecognition()
        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }

    /**
     * Hand the site URL off to the host activity, which navigates to the existing site-address
     * login screen with the URL prefilled and validation auto-started.
     */
    private fun routeToSiteAddressEntry(siteUrl: String) {
        scannerViewModel.stopCodesRecognition()
        requireNotNull(listener) {
            "${requireActivity().javaClass.simpleName} must implement QrLoginScannerFragment.Listener"
        }.onQrLoginSiteUrlPrefill(siteUrl)
    }

    private fun routeToAppLoginCredentials(siteUrl: String, username: String) {
        scannerViewModel.stopCodesRecognition()
        requireNotNull(listener) {
            "${requireActivity().javaClass.simpleName} must implement QrLoginScannerFragment.Listener"
        }.onQrLoginAppLoginCredentials(siteUrl, username)
    }

    private fun routeToAppLoginWpComEmail(siteUrl: String, wpComEmail: String) {
        scannerViewModel.stopCodesRecognition()
        requireNotNull(listener) {
            "${requireActivity().javaClass.simpleName} must implement QrLoginScannerFragment.Listener"
        }.onQrLoginAppLoginWpComEmail(siteUrl, wpComEmail)
    }
}
