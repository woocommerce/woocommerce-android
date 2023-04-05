package com.woocommerce.android.ui.jitm

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.extensions.hide
import com.woocommerce.android.extensions.show
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.jitm.JitmViewModel.Companion.JITM_MESSAGE_PATH_KEY
import com.woocommerce.android.ui.payments.banner.Banner
import com.woocommerce.android.ui.payments.banner.BannerState
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JitmFragment : Fragment() {
    private val viewModel: JitmViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        ComposeView(requireContext())

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.jitmState.observe(viewLifecycleOwner) { bannerState ->
            applyBannerComposeUI(bannerState)
        }
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is JitmViewModel.CtaClick -> {
                    startActivity(
                        Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(event.url)
                        ).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                    )
                }
                else -> event.isHandled = false
            }
        }
    }

    fun refreshJitms() {
        viewModel.fetchJitms()
    }

    private fun applyBannerComposeUI(state: BannerState) {
        if (state is BannerState.DisplayBannerState) {
            (requireView() as ComposeView).apply {
                show()
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
                setContent {
                    WooThemeWithBackground {
                        Banner(bannerState = state)
                    }
                }
            }
        } else {
            requireView().hide()
        }
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
