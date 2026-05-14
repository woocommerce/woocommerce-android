package com.woocommerce.android.ui.aiassistant

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.navOptions
import com.woocommerce.android.AppPrefsWrapper
import com.woocommerce.android.NavGraphMainDirections
import com.woocommerce.android.R
import com.woocommerce.android.aiassistant.ui.AssistantRoute
import com.woocommerce.android.aiassistant.ui.cards.AssistantCardAction
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.ui.feedback.SurveyType
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

    @Inject
    lateinit var appPrefsWrapper: AppPrefsWrapper

    override val activityAppBarStatus: AppBarStatus = AppBarStatus.Hidden

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return composeView {
            var showEarlyAccessNotice by remember {
                mutableStateOf(!appPrefsWrapper.isAiAssistantEarlyAccessNoticeDismissed)
            }

            AssistantRoute(
                onBack = { findNavController().navigateUp() },
                showEarlyAccessNotice = showEarlyAccessNotice,
                onDismissEarlyAccessNotice = {
                    appPrefsWrapper.isAiAssistantEarlyAccessNoticeDismissed = true
                    showEarlyAccessNotice = false
                },
                onEarlyAccessFeedbackClick = ::openAiAssistantFeedbackSurvey,
                assistantCardRenderer = WooAssistantCardRenderer(currencyFormatter),
                onCardAction = ::onCardAction,
            )
        }
    }

    private fun openAiAssistantFeedbackSurvey() {
        NavGraphMainDirections
            .actionGlobalFeedbackSurveyFragment(SurveyType.AI_ASSISTANT)
            .apply { findNavController().navigateSafely(this) }
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
                        findNavController().navigate(target.uri.toUri(), assistantCardDeepLinkNavOptions())
                    }
                }
            }
        }
    }
}

internal fun assistantCardDeepLinkNavOptions(): NavOptions = navOptions {
    anim {
        enter = R.anim.default_enter_anim
        exit = R.anim.default_exit_anim
        popEnter = R.anim.default_pop_enter_anim
        popExit = R.anim.default_pop_exit_anim
    }
}
