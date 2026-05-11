package com.woocommerce.android.ui.login.qrlogin

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.DynamicEdgeToEdgeActivity
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import com.woocommerce.android.util.WooPermissionUtils
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * QR-first login prologue shown after the merchant taps "Login to Store" when
 * [com.woocommerce.android.util.FeatureFlag.QR_LOGIN] is on.
 *
 * Hosts the (Activity-bound) camera-permission launcher and routes Android-level side-effects
 * dispatched by [QrLoginPrologueViewModel] — the screen and view model stay free of framework
 * concerns.
 */
@AndroidEntryPoint
class QrLoginPrologueFragment : Fragment() {
    companion object {
        const val TAG = "qr-login-prologue-fragment"
    }

    interface Listener {
        fun onQrLoginScanClicked()
        fun onQrLoginFallbackClicked()
    }

    private val viewModel: QrLoginPrologueViewModel by viewModels()

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    private var listener: Listener? = null

    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onCameraPermissionResult(
                granted = granted,
                shouldShowRationale = shouldShowRequestPermissionRationale(Manifest.permission.CAMERA),
            )
        }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeView {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()
        QrLoginPrologueScreen(
            cameraPermissionDialog = uiState.cameraPermissionDialog,
            onScanClicked = { viewModel.onScanClicked(isCameraPermissionGranted()) },
            onFallbackClicked = viewModel::onFallbackClicked,
            onCameraDenialPrimaryClicked = viewModel::onCameraDenialPrimaryClicked,
            onCameraDenialCancelled = viewModel::onCameraDenialCancelled,
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? DynamicEdgeToEdgeActivity)?.enableDynamicEdgeToEdge(forceDarkStatusBar = true)
        if (savedInstanceState == null) {
            viewModel.onPrologueShown()
        }
        observeEvents()
    }

    override fun onResume() {
        super.onResume()
        unifiedLoginTracker.setFlowAndStep(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_PROLOGUE)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? Listener
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    private fun observeEvents() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is QrLoginPrologueViewModel.Dispatch.LaunchCameraPermissionRequest ->
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                is QrLoginPrologueViewModel.Dispatch.OpenAppSettings ->
                    WooPermissionUtils.showAppSettings(requireContext(), openInNewStack = false)
                is QrLoginPrologueViewModel.Dispatch.NavigateToScanner ->
                    listener?.onQrLoginScanClicked()
                is QrLoginPrologueViewModel.Dispatch.NavigateToSiteAddressLogin ->
                    listener?.onQrLoginFallbackClicked()
            }
        }
    }

    private fun isCameraPermissionGranted(): Boolean =
        ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
}
