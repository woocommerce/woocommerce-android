package com.woocommerce.android.ui.jitm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.jitm.JitmViewModel.Companion.JITM_MESSAGE_PATH_KEY
import com.woocommerce.android.ui.payments.banner.Banner
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JitmFragment : Fragment() {
    private val viewModel: JitmViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        view as ComposeView
        view.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        viewModel.jitmState.observe(viewLifecycleOwner) { state ->
            view.setContent {
                WooThemeWithBackground {
                    when (state) {
                        is JitmState.Banner -> Banner(state)
                        is JitmState.Modal -> JitmModal(state)
                        JitmState.Hidden -> Spacer(modifier = Modifier.height(0.dp))
                    }
                }
            }
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is JitmViewModel.CtaClick -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }
                else -> event.isHandled = false
            }
        }
    }

    fun refreshJitms() {
        viewModel.fetchJitms()
    }

    companion object {
        fun newInstance(jitmMessagePath: String) =
            JitmFragment().apply {
                arguments = Bundle().apply {
                    putString(JITM_MESSAGE_PATH_KEY, jitmMessagePath)
                }
            }
    }
}
