package com.woocommerce.android.ui.jitm

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.jitm.JitmViewModel.Companion.JITM_MESSAGE_PATH_KEY
import com.woocommerce.android.util.ChromeCustomTabUtils
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class JitmFragment : Fragment() {
    private val viewModel: JitmViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ) = composeView {
        val state by viewModel.jitmState.observeAsState()
        when (val currentState = state) {
            is JitmState.Banner -> JitmBanner(currentState)
            is JitmState.Modal -> JitmModal(currentState)
            JitmState.Hidden, null -> Spacer(modifier = Modifier.height(0.dp))
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
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
