package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.aiassistant.ui.AssistantRoute
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.CurrencyFormatter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class AiAssistantHostFragment : BaseFragment() {
    @Inject
    lateinit var currencyFormatter: CurrencyFormatter

    @Inject
    internal lateinit var cardActionNavigator: WooAssistantCardActionNavigator

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
                assistantCardRenderer = WooAssistantCardRenderer(currencyFormatter),
                onCardAction = ::onCardAction,
            )
        }
    }

    private fun onCardAction(action: AssistantCardAction) {
        viewLifecycleOwner.lifecycleScope.launch {
            val target = cardActionNavigator.targetFor(action) ?: return@launch
            if (viewLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                when (target) {
                    is WooAssistantCardNavigationTarget.Direction -> {
                        findNavController().navigateSafely(target.directions)
                    }
                    is WooAssistantCardNavigationTarget.DeepLink -> {
                        findNavController().navigate(target.uri.toUri())
                    }
                }
            }
        }
    }

    companion object {
        private const val ASSISTANT_CONVERSATION_ID = "dashboard-assistant"
    }
}
