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
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.NewStore
import com.woocommerce.android.ui.login.storecreation.countrypicker.CountryListPickerViewModel.NavigateToSummaryStep
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent.Event.Exit
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class CountryListPickerFragment : BaseFragment() {
    private val viewModel: CountryListPickerViewModel by viewModels()
    @Inject lateinit var newStore: NewStore

    override
    val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)

            setContent {
                WooThemeWithBackground {
                    CountryListPickerScreen(viewModel = viewModel)
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
                is Exit -> findNavController().popBackStack()
                is NavigateToSummaryStep -> navigateToInstallationStep()
            }
        }
    }

    private fun navigateToInstallationStep() {
        findNavController().navigateSafely(
            CountryListPickerFragmentDirections.actionCountryListPickerFragmentToSummaryFragment()
        )
    }
}
