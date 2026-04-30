package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.core.loop.ToolScope
import com.woocommerce.android.aiassistant.runtime.AssistantRuntime
import com.woocommerce.android.aiassistant.ui.AssistantChatScreen
import com.woocommerce.android.aiassistant.ui.AssistantViewModel
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiAssistantHostFragment : BaseFragment() {
    @Inject lateinit var assistantRuntime: AssistantRuntime
    @Inject lateinit var selectedSite: SelectedSite

    private val viewModel: AssistantViewModel by viewModels {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return AssistantViewModel(
                    runtime = assistantRuntime,
                    conversationId = ASSISTANT_CONVERSATION_ID,
                    siteId = selectedSite.get().siteId,
                    toolScope = ToolScope.GLOBAL,
                ) as T
            }
        }
    }

    override fun getFragmentTitle() = getString(R.string.more_menu_button_ai_assistant)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            AssistantChatScreen(
                viewModel = viewModel,
            )
        }
    }

    companion object {
        private const val ASSISTANT_CONVERSATION_ID = "more-menu-assistant"
    }
}
