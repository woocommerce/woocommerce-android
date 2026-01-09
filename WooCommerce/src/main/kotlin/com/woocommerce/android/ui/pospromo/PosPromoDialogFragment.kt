package com.woocommerce.android.ui.pospromo

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
class PosPromoDialogFragment : DialogFragment() {

    private val viewModel: PosPromoViewModel by viewModels()

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
                    PosPromoCarouselDialog(
                        state = state,
                        onDismiss = { dismiss() },
                        onNextClick = viewModel::onNextClick,
                        onExploreClick = {
                            ChromeCustomTabUtils.launchUrl(requireContext(), PosPromoViewModel.WOO_POS_DOCS_URL)
                            dismiss()
                        }
                    )
                }
            }
        }
    }

    companion object {
        private const val TAG = "PosPromoDialogFragment"

        fun show(fragmentManager: FragmentManager) {
            PosPromoDialogFragment().show(fragmentManager, TAG)
        }
    }
}
