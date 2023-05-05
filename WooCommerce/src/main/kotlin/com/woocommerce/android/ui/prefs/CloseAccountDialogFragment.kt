package com.woocommerce.android.ui.prefs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material.Text
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.R.style
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class CloseAccountDialogFragment : DialogFragment() {

    private val viewModel: CloseAccountViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = style.Woo_Animations_Dialog

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    viewModel.viewState.observeAsState().value?.let { state ->
                        Text(text = "Closing account for user ${state.userName}")
                    }
                }
            }
        }
    }
}
