package com.woocommerce.android.ui.jetpack

import android.os.Bundle
import android.view.View
import androidx.core.text.HtmlCompat
import androidx.fragment.app.DialogFragment
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.databinding.DialogJetpackInstallProgressBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.tools.SelectedSite
import com.woocommerce.android.util.WooAnimUtils
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class JetpackInstallProgressDialog : DialogFragment(R.layout.dialog_jetpack_install_progress) {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    @Inject lateinit var selectedSite: SelectedSite

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) R.style.Theme_Woo_Dialog else R.style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        val binding = DialogJetpackInstallProgressBinding.bind(view)

        with(binding.subtitle) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(context.getString(R.string.jetpack_install_start_default_name))

            selectedSite.get().name?.let {
                stringBuilder.append(" <b>${selectedSite.get().name}</b> ",)
            }

            text = HtmlCompat.fromHtml(
                context.getString(R.string.jetpack_install_progress_subtitle, stringBuilder.toString()),
                HtmlCompat.FROM_HTML_MODE_LEGACY
            )
        }

        binding.jetpackProgressActionButton.setOnClickListener {
            findNavController().navigateSafely(
                JetpackInstallProgressDialogDirections.actionJetpackInstallProgressDialogToDashboard()
            )
        }

        // Temporary animation for testing the UI. Replace with actual animation when it's ready.
        WooAnimUtils.rotate(binding.secondStepIcon)
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
