package com.woocommerce.android.ui.jetpack

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.analytics.AnalyticsEvent
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.databinding.DialogJetpackBenefitsBinding
import com.woocommerce.android.extensions.navigateSafely
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils

@AndroidEntryPoint
class JetpackBenefitsDialog : DialogFragment(R.layout.dialog_jetpack_benefits) {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) R.style.Theme_Woo_Dialog else R.style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogJetpackBenefitsBinding.bind(view)
        binding.dismissButton.setOnClickListener {
            dismiss()
        }
        binding.installJetpackButton.setOnClickListener {
            findNavController().navigateSafely(
                JetpackBenefitsDialogDirections.actionJetpackBenefitsDialogToJetpackInstallStartDialog()
            )
            AnalyticsTracker.track(
                stat = AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
            )
        }
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
