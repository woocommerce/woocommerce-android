package com.woocommerce.android.ui.prefs.domain

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.viewModels
import androidx.hilt.navigation.fragment.hiltNavGraphViewModels
import androidx.navigation.fragment.findNavController
import com.woocommerce.android.R
import com.woocommerce.android.extensions.navigateToHelpScreen
import com.woocommerce.android.ui.base.BaseFragment
import com.woocommerce.android.ui.compose.theme.WooThemeWithBackground
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerScreen
import com.woocommerce.android.ui.login.storecreation.domainpicker.DomainPickerViewModel
import com.woocommerce.android.ui.main.AppBarStatus
import com.woocommerce.android.ui.prefs.domain.DomainChangeViewModel.ShowMoreAboutDomains
import com.woocommerce.android.util.ChromeCustomTabUtils
import com.woocommerce.android.viewmodel.MultiLiveEvent
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DomainSearchFragment : BaseFragment() {
    private val domainChangeViewModel: DomainChangeViewModel by hiltNavGraphViewModels(R.id.nav_graph_domain_change)
    private val domainSearchViewModel: DomainPickerViewModel by viewModels()

    override val activityAppBarStatus: AppBarStatus
        get() = AppBarStatus.Hidden

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                WooThemeWithBackground {
                    DomainPickerScreen(domainSearchViewModel, ::onDomainSelected)
                }
            }
        }
    }

    private fun onDomainSelected(domain: String) {
        domainChangeViewModel.onDomainSelected(domain)
        domainSearchViewModel.onDomainSuggestionSelected(domain)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupObservers()
    }

    private fun setupObservers() {
        domainSearchViewModel.event.observe(viewLifecycleOwner) { event ->
            when (event) {
                is MultiLiveEvent.Event.Exit -> findNavController().popBackStack()
                is MultiLiveEvent.Event.NavigateToHelpScreen -> navigateToHelpScreen(event.origin)
                is ShowMoreAboutDomains -> ChromeCustomTabUtils.launchUrl(requireContext(), event.url)
            }
        }
    }
}
