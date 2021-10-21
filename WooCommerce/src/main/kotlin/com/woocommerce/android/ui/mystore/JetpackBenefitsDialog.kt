package com.woocommerce.android.ui.mystore

import android.os.Bundle
import android.view.View
import androidx.fragment.app.DialogFragment
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackBenefitsBinding
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
        super.onViewCreated(view, savedInstanceState)

        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogJetpackBenefitsBinding.bind(view)
        binding.dismissButton.setOnClickListener {
            dismiss()
        }
        binding.installJetpackButton.setOnClickListener {
            // TODO navigate to the jetpack installation screen
        }
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            requireDialog().window!!.setLayout(
                (DisplayUtils.getDisplayPixelWidth() * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getDisplayPixelHeight(context) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
