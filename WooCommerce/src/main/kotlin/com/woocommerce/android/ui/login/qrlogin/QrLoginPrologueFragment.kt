package com.woocommerce.android.ui.login.qrlogin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTrackerWrapper
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.login.DynamicEdgeToEdgeActivity
import com.woocommerce.android.ui.login.UnifiedLoginTracker
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * QR-first login prologue shown after the merchant taps "Login to Store" when
 * [com.woocommerce.android.util.FeatureFlag.QR_LOGIN] is on.
 *
 * Offers a single primary action ("Scan QR code") and a fallback link into the existing
 * credentials flow for merchants without access to their computer.
 */
@AndroidEntryPoint
class QrLoginPrologueFragment : Fragment() {
    companion object {
        const val TAG = "qr-login-prologue-fragment"
        private const val KEY_CAMERA_PERMISSION_DIALOG_STATE = "state"
        private const val VALUE_CAMERA_PERMISSION_STATE_FIRST_DENIAL = "first_denial"
        private const val VALUE_CAMERA_PERMISSION_STATE_PERMANENTLY_DENIED = "permanently_denied"
        private const val VALUE_CAMERA_PERMISSION_STATE_HIDDEN = "hidden"
    }

    interface Listener {
        fun onQrLoginScanClicked()
        fun onQrLoginFallbackClicked()
    }

    @Inject
    lateinit var unifiedLoginTracker: UnifiedLoginTracker

    @Inject
    lateinit var analyticsTracker: AnalyticsTrackerWrapper

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = composeView {
        QrLoginPrologueScreen(
            onScanClicked = {
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_SCAN_TAPPED)
                unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.LOGIN_QR_SCAN)
                listener?.onQrLoginScanClicked()
            },
            onFallbackClicked = {
                analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_FALLBACK_TAPPED)
                unifiedLoginTracker.trackClick(UnifiedLoginTracker.Click.LOGIN_QR_FALLBACK)
                listener?.onQrLoginFallbackClicked()
            },
            onCameraPermissionDialogShown = { state ->
                analyticsTracker.track(
                    AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DIALOG_SHOWN,
                    mapOf(KEY_CAMERA_PERMISSION_DIALOG_STATE to state.toAnalyticsValue())
                )
            },
            onCameraPermissionDialogPrimary = { state ->
                analyticsTracker.track(
                    AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_PRIMARY_TAPPED,
                    mapOf(KEY_CAMERA_PERMISSION_DIALOG_STATE to state.toAnalyticsValue())
                )
            },
            onCameraPermissionDialogDismissed = { state ->
                analyticsTracker.track(
                    AnalyticsEvent.LOGIN_QR_PROLOGUE_CAMERA_PERMISSION_DISMISSED,
                    mapOf(KEY_CAMERA_PERMISSION_DIALOG_STATE to state.toAnalyticsValue())
                )
            },
        )
    }

    private fun CameraDenialState.toAnalyticsValue(): String = when (this) {
        CameraDenialState.FirstDenial -> VALUE_CAMERA_PERMISSION_STATE_FIRST_DENIAL
        CameraDenialState.PermanentlyDenied -> VALUE_CAMERA_PERMISSION_STATE_PERMANENTLY_DENIED
        CameraDenialState.Hidden -> VALUE_CAMERA_PERMISSION_STATE_HIDDEN
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? DynamicEdgeToEdgeActivity)?.enableDynamicEdgeToEdge(forceDarkStatusBar = true)
        if (savedInstanceState == null) {
            analyticsTracker.track(AnalyticsEvent.LOGIN_QR_PROLOGUE_SHOWN)
            unifiedLoginTracker.track(UnifiedLoginTracker.Flow.LOGIN_QR, UnifiedLoginTracker.Step.QR_PROLOGUE)
        }
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
}
