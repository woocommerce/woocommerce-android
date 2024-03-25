package com.woocommerce.android.ui.onboarding.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.util.ChromeCustomTabUtils

class PaymentsPreSetupFragment : BaseFragment() {
    private val args: PaymentsPreSetupFragmentArgs by navArgs()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    PaymentsPreSetupScreen(
                        isWooPaymentsTask = args.taskId == OnboardingTaskType.WC_PAYMENTS.id,
                        backButtonClick = { findNavController().popBackStack() },
                        onTermsOfServiceClick = { onTermsOfServiceClick() },
                        onPrivacyPolicyClick = { onPrivacyPolicyClick() },
                        onContinueButtonClick = { onContinueButtonClick() },
                        onLearnMoreButtonClick = { onLearnMoreButtonClick() }
                    )
                }
            }
        }
    }

    private fun onTermsOfServiceClick() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.AUTOMATTIC_TOS)
    }

    private fun onPrivacyPolicyClick() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.AUTOMATTIC_PRIVACY_POLICY)
    }

    private fun onContinueButtonClick() {
        if (args.taskId == OnboardingTaskType.WC_PAYMENTS.id) {
            AnalyticsTracker.track(AnalyticsEvent.STORE_ONBOARDING_WCPAY_TERMS_CONTINUE_TAPPED)
        }
        findNavController().navigateSafely(
            directions = PaymentsPreSetupFragmentDirections.actionPaymentsPreSetupFragmentToGetPaidFragment(
                taskId = args.taskId
            )
        )
    }

    private fun onLearnMoreButtonClick() {
        val url = when (args.taskId) {
            OnboardingTaskType.WC_PAYMENTS.id -> AppUrls.STORE_ONBOARDING_WCPAY_SETUP_GUIDE
            OnboardingTaskType.PAYMENTS.id -> AppUrls.STORE_ONBOARDING_PAYMENTS_SETUP_GUIDE
            else -> error("Wrong task ID passed to the screen")
        }

        ChromeCustomTabUtils.launchUrl(requireContext(), url)
    }
}
