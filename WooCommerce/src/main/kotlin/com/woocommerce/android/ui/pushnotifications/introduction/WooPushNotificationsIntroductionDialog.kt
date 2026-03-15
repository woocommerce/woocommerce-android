package com.woocommerce.android.ui.pushnotifications.introduction

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.compose.composeView
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.NavigateToHelpScreen
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.util.DisplayUtils

@AndroidEntryPoint
class WooPushNotificationsIntroductionDialog : DialogFragment() {
    companion object {
        private const val TABLET_LANDSCAPE_WIDTH_RATIO = 0.35f
        private const val TABLET_LANDSCAPE_HEIGHT_RATIO = 0.8f
    }

    private val viewModel: WooPushNotificationsIntroductionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, if (isTabletLandscape()) R.style.Theme_Woo_Dialog else R.style.Theme_Woo)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dialog?.window?.attributes?.windowAnimations = R.style.Woo_Animations_Dialog

        return composeView {
            WooPushNotificationsIntroductionScreen(viewModel)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is WooPushNotificationsIntroductionViewModel.NavigateToConnectionSteps -> {
                    findNavController().navigateSafely(
                        WooPushNotificationsIntroductionDialogDirections
                            .actionWooPushNotificationsIntroductionDialogToWooPushNotificationsConnectionStepsFragment(
                                isSiteConnectedToJetpack = event.isSiteConnectedToJetpack,
                                shouldAutoOpenUpdatePlugin = event.shouldAutoOpenUpdatePlugin
                            )
                    )
                }

                is NavigateToHelpScreen -> navigateToHelpScreen(event.origin)

                is WooPushNotificationsIntroductionViewModel.OpenUrlEvent -> {
                    ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
                }

                Exit -> findNavController().navigateUp()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (isTabletLandscape()) {
            requireDialog().window?.setLayout(
                (DisplayUtils.getWindowPixelWidth(requireContext()) * TABLET_LANDSCAPE_WIDTH_RATIO).toInt(),
                (DisplayUtils.getWindowPixelHeight(requireContext()) * TABLET_LANDSCAPE_HEIGHT_RATIO).toInt()
            )
        }
    }

    private fun isTabletLandscape() = (DisplayUtils.isTablet(context) || DisplayUtils.isXLargeTablet(context)) &&
        DisplayUtils.isLandscape(context)
}
