package com.woocommerce.android.ui.payments.scantopay

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground

class ScanToPayDialogFragment : DialogFragment() {
    private val args: ScanToPayDialogFragmentArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    ScanToPayScreen(args.qrContent) {
                        navigateBackWithNotice(KEY_SCAN_TO_PAY_RESULT)
                    }
                }
            }
        }
    }

    companion object {
        const val KEY_SCAN_TO_PAY_RESULT = "key_scan_to_pay_result"
    }
}
