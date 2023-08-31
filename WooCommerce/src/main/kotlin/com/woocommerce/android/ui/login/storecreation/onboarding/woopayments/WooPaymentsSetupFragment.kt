package com.woocommerce.android.ui.login.storecreation.onboarding.woopayments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.navigation.findNavController
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.AppUrls
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.onboarding.StoreOnboardingRepository.OnboardingTaskType.WC_PAYMENTS
import com.woocommerce.android.util.ChromeCustomTabUtils
import org.wordpress.android.util.DisplayUtils

class WooPaymentsSetupFragment : DialogFragment() {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) R.style.Theme_Woo_Dialog else R.style.Theme_Woo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

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

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            requireDialog().window!!.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
