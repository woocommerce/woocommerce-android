package com.woocommerce.android.ui.prefs.privacy.banner

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.widgets.WCBottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PrivacyBannerFragment : WCBottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                WooThemeWithBackground {
                    PrivacyBannerScreen()
                }
            }
        }
    }
}
