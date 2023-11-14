package com.woocommerce.android.ui.payments.scantopay

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.extensions.currentScreenBrightness
import com.woocommerce.android.extensions.navigateBackWithNotice
import com.woocommerce.android.ui.compose.theme.WooTheme
import kotlin.properties.Delegates

class ScanToPayDialogFragment : DialogFragment() {
    private val args: ScanToPayDialogFragmentArgs by navArgs()

    private var initialScreenBrightness by Delegates.notNull<Float>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooTheme {
                    ScanToPayScreen(args.qrContent) {
                        navigateBackWithNotice(KEY_SCAN_TO_PAY_RESULT)
                    }
                }
            }
        }
    }

    override fun onResume() {
        with(requireActivity()) {
            initialScreenBrightness = currentScreenBrightness
            currentScreenBrightness = MAX_BRIGHTNESS
        }
        super.onResume()
    }

    override fun onPause() {
        requireActivity().currentScreenBrightness = initialScreenBrightness
        super.onPause()
    }

    companion object {
        const val KEY_SCAN_TO_PAY_RESULT = "key_scan_to_pay_result"
        private const val MAX_BRIGHTNESS = 1F
    }
}
