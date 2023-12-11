package com.woocommerce.android.ui.themes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.handleNotice
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.profiler.BaseStoreProfilerViewModel.NavigateToNextStep
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.themes.ThemePickerViewModel.NavigateToThemePreview
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ThemePickerFragment : BaseFragment() {
    private val viewModel: ThemePickerViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    ThemePickerScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
        handleResults()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is NavigateToNextStep -> navigateToStoreInstallationStep()
                is NavigateToThemePreview -> navigateToThemePreviewFragment(event)
            }
        }
    }

    private fun handleResults() {
        handleNotice(ThemePreviewFragment.THEME_SELECTED_NOTICE) {
            navigateToStoreInstallationStep()
        }
    }

    private fun navigateToStoreInstallationStep() {
        findNavController().navigateSafely(
            ThemePickerFragmentDirections
                .actionThemePickerFragmentToStoreCreationInstallationFragment()
        )
    }

    private fun navigateToThemePreviewFragment(event: NavigateToThemePreview) {
        findNavController().navigateSafely(
            ThemePickerFragmentDirections
                .actionThemePickerFragmentToThemePreviewFragment(
                    themeId = event.themeId,
                    isFromStoreCreation = event.isFromStoreCreation
                )
        )
    }
}
