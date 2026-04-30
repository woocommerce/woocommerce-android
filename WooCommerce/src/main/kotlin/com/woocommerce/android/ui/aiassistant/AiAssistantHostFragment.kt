package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.AssistantRoute
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiAssistantHostFragment : BaseFragment() {
    override fun getFragmentTitle() = getString(R.string.more_menu_button_ai_assistant)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            AssistantRoute(conversationId = ASSISTANT_CONVERSATION_ID)
        }
    }

    companion object {
        private const val ASSISTANT_CONVERSATION_ID = "more-menu-assistant"
    }
}
