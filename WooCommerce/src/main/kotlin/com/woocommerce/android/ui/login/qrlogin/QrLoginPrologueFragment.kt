package com.woocommerce.android.ui.login.qrlogin

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.DynamicEdgeToEdgeActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * QR-first login prologue shown when [com.woocommerce.android.util.FeatureFlag.QR_LOGIN] is on.
 *
 * Offers a single primary action ("Scan QR code") and a fallback link into the existing
 * credentials flow for merchants without access to their computer.
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

    private var listener: Listener? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            WooThemeWithBackground {
                QrLoginPrologueScreen(
                    onScanClicked = { listener?.onQrLoginScanClicked() },
                    onFallbackClicked = { listener?.onQrLoginFallbackClicked() }
                )
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? DynamicEdgeToEdgeActivity)?.enableDynamicEdgeToEdge(forceDarkStatusBar = true)
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
