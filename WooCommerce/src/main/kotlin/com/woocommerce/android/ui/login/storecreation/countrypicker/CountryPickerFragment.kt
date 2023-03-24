package com.woocommerce.android.ui.login.storecreation.countrypicker

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
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.countrypicker.CountryPickerViewModel.NavigateToDomainPickerStep
import com.woocommerce.android.ui.login.storecreation.countrypicker.CountryPickerViewModel.NavigateToInstallationStep
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CountryPickerFragment : BaseFragment() {
    private val viewModel: CountryPickerViewModel by viewModels()
    @Inject lateinit var newStore: NewStore

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    CountryPickerScreen(viewModel)
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
                is NavigateToDomainPickerStep -> navigateToDomainPickerStep()
                is NavigateToInstallationStep -> navigateToInstallationStep()
            }
        }
    }

    private fun navigateToInstallationStep() {
        findNavController().navigateSafely(
            CountryPickerFragmentDirections.actionCountryPickerFragmentToInstallationFragment()
        )
    }

    private fun navigateToDomainPickerStep() {
        findNavController().navigateSafely(
            CountryPickerFragmentDirections.actionCountryPickerFragmentToDomainPickerFragment(
                initialQuery = newStore.data.name ?: ""
            )
        )
    }
}
