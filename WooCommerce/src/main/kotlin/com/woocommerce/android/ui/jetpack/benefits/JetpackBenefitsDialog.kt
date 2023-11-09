package com.woocommerce.android.ui.jetpack.benefits

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R.style
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.UIMessageResolver
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.ShowSnackbar
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils
import javax.inject.Inject

@AndroidEntryPoint
class JetpackBenefitsDialog : DialogFragment() {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    private val viewModel: JetpackBenefitsViewModel by viewModels()

    @Inject
    lateinit var uiMessageResolver: UIMessageResolver

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Use fullscreen style for all cases except tablet in landscape mode
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) style.Theme_Woo_Dialog else style.Theme_Woo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Specify transition animations
        dialog?.window?.attributes?.windowAnimations = style.Woo_Animations_Dialog

        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    JetpackBenefitsScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is JetpackBenefitsViewModel.StartJetpackActivationForJetpackCP -> {
                    findNavController().navigateSafely(
                        JetpackBenefitsDialogDirections.actionJetpackBenefitsDialogToJetpackCPInstallStartDialog()
                    )
                }

                is JetpackBenefitsViewModel.StartJetpackActivationForApplicationPasswords -> {
                    findNavController().navigateSafely(
                        JetpackBenefitsDialogDirections.actionJetpackBenefitsDialogToJetpackActivation(
                            siteUrl = event.siteUrl,
                            jetpackStatus = event.jetpackStatus
                        )
                    )
                }

                is JetpackBenefitsViewModel.OpenWpAdminJetpackActivation -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.activationUrl)
                }

                is JetpackBenefitsViewModel.OpenJetpackEligibilityError -> {
                    findNavController().navigateSafely(
                        JetpackBenefitsDialogDirections
                            .actionJetpackBenefitsDialogToJetpackActivationEligibilityErrorFragment(
                                username = event.username,
                                role = event.role
                            )
                    )
                }

                is ShowSnackbar -> uiMessageResolver.showSnack(event.message)
                Exit -> findNavController().navigateUp()
            }
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
