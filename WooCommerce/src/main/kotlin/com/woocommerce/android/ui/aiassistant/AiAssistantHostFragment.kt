package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.aiassistant.ui.AssistantRoute
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.CurrencyFormatter
import com.woocommerce.android.tools.SelectedSite
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class AiAssistantHostFragment : BaseFragment() {
    @Inject lateinit var currencyFormatter: CurrencyFormatter
    @Inject lateinit var selectedSite: SelectedSite

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
                onCardAction = { action ->
                    action.toNavDirections(selectedSite.get())?.let { direction ->
                        findNavController().navigateSafely(direction)
                    }
                },
            )
        }
    }

    companion object {
        private const val ASSISTANT_CONVERSATION_ID = "more-menu-assistant"
    }
}
