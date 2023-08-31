package com.woocommerce.android.ui.login.storecreation.onboarding.woopayments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils

class WooPaymentsSetupFragment : BaseFragment() {

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    WooPaymentsSetupScreen(
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
        findNavController().navigateSafely(
            directions = WooPaymentsSetupFragmentDirections.actionWooPaymentsSetupFragmentToGetPaidFragment(
                taskId = WC_PAYMENTS.id
            )
        )
    }

    private fun onLearnMoreButtonClick() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.STORE_ONBOARDING_WCPAY_SETUP_GUIDE)
    }
}
