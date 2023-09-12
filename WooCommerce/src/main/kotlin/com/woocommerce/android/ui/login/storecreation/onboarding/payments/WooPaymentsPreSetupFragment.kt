package com.woocommerce.android.ui.login.storecreation.onboarding.payments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.util.ChromeCustomTabUtils

class WooPaymentsPreSetupFragment : BaseFragment() {

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    WooPaymentsPreSetupScreen(
                        onCloseButtonClick = { findNavController().popBackStack() },
                        onWPComAccountMoreDetailsClick = { onWPComAccountMoreDetailsClick() },
                        onBeginButtonClick = { onBeginButtonClick() },
                        onLearnMoreClick = { onLearnMoreClick() }
                    )
                }
            }
        }
    }

    private fun onWPComAccountMoreDetailsClick() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.STORE_ONBOARDING_WCPAY_PRE_SETUP_WPCOM_ACCOUNT)
    }

    private fun onLearnMoreClick() {
        ChromeCustomTabUtils.launchUrl(requireContext(), AppUrls.STORE_ONBOARDING_WCPAY_PRE_SETUP_LEARN_MORE)
    }

    private fun onBeginButtonClick() {
        AnalyticsTracker.track(AnalyticsEvent.STORE_ONBOARDING_WCPAY_BEGIN_SETUP_TAPPED)
        findNavController().navigateSafely(
            directions = WooPaymentsPreSetupFragmentDirections
                .actionWooPaymentsPreSetupFragmentToWooPaymentsSetupFragment(OnboardingTaskType.WC_PAYMENTS.id)
        )
    }
}
