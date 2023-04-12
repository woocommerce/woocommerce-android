package com.woocommerce.android.ui.login.storecreation.name

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.extensions.navigateSafely
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToDomainPicker
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToStoreInstallation
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToStoreProfiler
import com.woocommerce.android.ui.login.storecreation.name.StoreNamePickerViewModel.NavigateToSummary
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class StoreNamePickerFragment : BaseFragment() {
    private val viewModel: StoreNamePickerViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    StoreNamePickerScreen(viewModel)
                }
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        viewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is NavigateToDomainPicker -> navigateToDomainPicker(event.domainInitialQuery)
                is NavigateToStoreProfiler -> navigateToStoreProfiler()
                is NavigateToStoreInstallation -> navigateToInstallation()
                is NavigateToSummary -> navigateToSummary()
            }
        }
    }

    private fun navigateToSummary() {
        StoreNamePickerFragmentDirections.actionStoreNamePickerFragmentToSummaryFragment()
            .let { findNavController().navigateSafely(it) }
    }

    private fun navigateToInstallation() {
        StoreNamePickerFragmentDirections.actionStoreNamePickerFragmentToInstallationFragment()
            .let { findNavController().navigateSafely(it) }
    }

    private fun navigateToStoreProfiler() {
        StoreNamePickerFragmentDirections.actionStoreNamePickerFragmentToStoreProfilerCategoryFragment()
            .let { findNavController().navigateSafely(it) }
    }

    private fun navigateToDomainPicker(domainInitialQuery: String) {
        StoreNamePickerFragmentDirections.actionStoreNamePickerFragmentToDomainPickerFragment(
            domainInitialQuery
        ).let { findNavController().navigateSafely(it) }
    }
}
