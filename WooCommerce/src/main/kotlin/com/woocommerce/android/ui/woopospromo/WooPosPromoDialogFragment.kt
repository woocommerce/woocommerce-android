package com.woocommerce.android.ui.woopospromo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import com.woocommerce.android.R
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WooPosPromoDialogFragment : DialogFragment() {

    private val viewModel: WooPosPromoViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Theme_Woo_Dialog_RoundedCorners_NoMinWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    val state by viewModel.state.collectAsState()
                    WooPosPromoCarouselModal(
                        state = state,
                        onDismiss = { dismiss() },
                        onExploreClick = {
                            ChromeCustomTabUtils.launchUrl(requireContext(), WooPosPromoViewModel.WOO_POS_DOCS_URL)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "WooPosPromoDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            WooPosPromoDialogFragment().show(fragmentManager, TAG)
        }
    }
}