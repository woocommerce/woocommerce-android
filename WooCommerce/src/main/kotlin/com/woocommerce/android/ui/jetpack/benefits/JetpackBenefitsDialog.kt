package com.woocommerce.android.ui.jetpack.benefits

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R.layout
import com.woocommerce.android.R.string
import com.woocommerce.android.R.style
import com.woocommerce.android.analytics.AnalyticsEvent.JETPACK_INSTALL_BUTTON_TAPPED
import com.woocommerce.android.analytics.AnalyticsTracker
import com.woocommerce.android.analytics.AnalyticsTracker.Companion
import com.woocommerce.android.databinding.DialogJetpackBenefitsBinding
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.jetpack.JetpackBenefitsDialogDirections
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import com.woocommerce.android.widgets.CustomProgressDialog
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class JetpackBenefitsDialog : DialogFragment(layout.dialog_jetpack_benefits) {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    private val viewModel: JetpackBenefitsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver
    private var progressDialog: CustomProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) style.Theme_Woo_Dialog else style.Theme_Woo)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = style.Woo_Animations_Dialog

        setupObservers()

        val binding = DialogJetpackBenefitsBinding.bind(view)
        binding.dismissButton.setOnClickListener {
            dismiss()
        }
        binding.installJetpackButton.setOnClickListener {
            viewModel.onInstallClick()
            AnalyticsTracker.track(
                stat = JETPACK_INSTALL_BUTTON_TAPPED,
                properties = mapOf(AnalyticsTracker.KEY_JETPACK_INSTALLATION_SOURCE to "benefits_modal")
            )
        }
    }

    private fun setupObservers() {
        viewModel.isLoadingDialogShown.observe(viewLifecycleOwner) { show ->
            if (show) {
                if (progressDialog?.isVisible == true) return@observe
                progressDialog?.dismiss()
                progressDialog = CustomProgressDialog.show(
                    getString(string.jetpack_benefits_fetching_status),
                    getString(string.please_wait)
                ).also {
                    it.isCancelable = false
                    it.show(childFragmentManager, CustomProgressDialog.TAG)
                }
            } else {
                progressDialog?.dismiss()
                progressDialog = null
            }
        }

        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is StartJetpackCPInstallation -> {
                    findNavController().navigateSafely(
                        JetpackBenefitsDialogDirections.actionJetpackBenefitsDialogToJetpackInstallStartDialog()
                    )
                }

                is StartApplicationPasswordsInstallation -> {
                    // TODO
                    Toast.makeText(
                        requireContext(),
                        "Jetpack Status: \n" +
                            "Installed: ${event.jetpackStatus.isJetpackInstalled}\n" +
                            "Connected: ${event.jetpackStatus.isJetpackConnected}",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        progressDialog?.dismiss()
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
