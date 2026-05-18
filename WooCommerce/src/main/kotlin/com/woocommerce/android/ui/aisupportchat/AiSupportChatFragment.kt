package com.woocommerce.android.ui.aisupportchat

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiSupportChatFragment : Fragment() {
    private val viewModel: AiSupportChatViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        composeView {
            AiSupportChatScreen(viewModel = viewModel)
        }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.onLaunchModeLoaded(AiSupportChatActivity.launchModeFrom(requireActivity().intent))
    }
}
