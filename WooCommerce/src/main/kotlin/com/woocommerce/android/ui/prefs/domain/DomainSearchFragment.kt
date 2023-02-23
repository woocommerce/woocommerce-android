package com.woocommerce.android.ui.prefs.domain

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
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion.Paid
import com.woocommerce.android.ui.common.domain.DomainSuggestionsRepository.DomainSuggestion.Premium
import com.woocommerce.android.ui.common.domain.DomainSuggestionsViewModel.NavigateToNextStep
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerScreen
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DomainSearchFragment : BaseFragment() {
    private val viewModel: DomainSearchViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    DomainPickerScreen(viewModel, viewModel::onDomainSuggestionSelected)
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
                is NavigateToNextStep -> navigateToContactForm(event.selectedSuggestion)
            }
        }
    }

    private fun navigateToContactForm(domain: DomainSuggestion) {
        when (domain) {
            is Paid -> {
                navigateToContactForm(domain.productId, domain.name)
            }
            is Premium -> {
                navigateToContactForm(domain.productId, domain.name)
            }
            else -> {}
        }
    }

    private fun navigateToContactForm(productId: Int, name: String) {
        findNavController().navigateSafely(
            DomainSearchFragmentDirections.actionDomainSearchFragmentToDomainRegistrationDetailsFragment(
                DomainProductDetails(productId, name)
            )
        )
    }
}
