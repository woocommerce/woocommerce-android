package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.aiassistant.ui.AssistantRoute
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class AiAssistantHostFragment : BaseFragment() {
    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            AssistantRoute(
                conversationId = ASSISTANT_CONVERSATION_ID,
                onBack = { findNavController().navigateUp() },
            )
        }
    }

    companion object {
        private const val ASSISTANT_CONVERSATION_ID = "more-menu-assistant"
    }
}
