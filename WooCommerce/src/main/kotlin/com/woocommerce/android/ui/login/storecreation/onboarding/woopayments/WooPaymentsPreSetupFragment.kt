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
import com.woocommerce.android.util.ChromeCustomTabUtils
import org.wordpress.android.util.DisplayUtils

class WooPaymentsPreSetupFragment : DialogFragment() {
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
        findNavController().navigateSafely(
            directions = WooPaymentsPreSetupFragmentDirections
                .actionWooPaymentsPreSetupFragmentToWooPaymentsSetupFragment()
        )
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
